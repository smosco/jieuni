# 문제 해결 기록

이 문서는 구현하면서 부딪힌 문제 세 가지를 **상황 → 원인 → 해결 → 확인 → 한계** 순서로 적습니다. 어떤 문제를 어떻게 찾고 검증했는지 볼 때 읽습니다. 각 결정의 요약은 [DECISIONS.md](DECISIONS.md)에 있습니다.

| 사례 | 한 줄 요약 |
| --- | --- |
| [1. 동시 요청](#1-동시-요청에서-중복-키-오류와-한도-초과) | "확인하고 쓰기" 사이의 틈을 upsert와 행 잠금으로 막고, 보호 장치를 뺐을 때 실패하는 테스트로 확인 |
| [2. 둘러보기 인덱스](#2-둘러보기-목록이-인덱스를-쓰지-않음) | 부분 인덱스와 행 비교 커서로 바꾸고, 2만 행 합성 데이터의 실행 계획으로 확인 |
| [3. 프록시 뒤 OAuth](#3-프록시-뒤-oauth-리디렉트) | 테스트로는 보이지 않던 리디렉트 주소 문제를 compose와 curl로 찾음 |

---

## 1. 동시 요청에서 중복 키 오류와 한도 초과

### 상황

저장 버튼의 첫 구현은 "`findById`로 찾아서 있으면 상태를 바꾸고, 없으면 새로 `save()`"였습니다. 팔로우는 `existsById` → `save()`였습니다. 테스트는 모두 통과했습니다. 그런데 더블 탭이나 재시도처럼 같은 요청이 동시에 오면 어떻게 되는지는 확인하지 않은 상태였습니다.

### 원인

두 요청이 거의 동시에 조회하면 둘 다 "없음"을 보고 둘 다 INSERT합니다. 두 번째 INSERT가 기본 키 충돌로 실패하고 500이 됩니다. 확인과 쓰기 사이에 틈이 있는 패턴입니다.

### 해결

PostgreSQL 네이티브 upsert로 바꿨습니다. 판단과 쓰기가 SQL 한 문장 안에서 일어나 틈이 없습니다.

```sql
-- 저장: 상태가 바뀔 때만 updated_at을 바꾼다
insert into saves (user_id, place_id, status, updated_at) values (:userId, :placeId, :status, :at)
on conflict (user_id, place_id) do update
  set status = excluded.status,
      updated_at = case when saves.status <> excluded.status then excluded.updated_at else saves.updated_at end

-- 팔로우
insert into follows (user_id, maker_id, created_at) values (:userId, :makerId, :at)
on conflict (user_id, maker_id) do nothing
```

`Save`·`Follow` 엔티티는 읽기용으로만 남겼습니다(`SaveRepository`, `FollowRepository`).

### 확인

- `ConcurrentSaveFollowTest`의 `concurrentSavesAllSucceedWithOneRow`, `concurrentFollowsAllSucceedWithOneRow`
  - 같은 요청 8개를 스레드 8개로, `CountDownLatch`로 동시에 출발시킵니다. 모두 성공하고 행은 하나입니다.
  - 테스트 트랜잭션은 커밋하지 않아 다른 스레드가 데이터를 볼 수 없습니다. 그래서 이 클래스만 `@Transactional(propagation = NOT_SUPPORTED)`로 실제로 커밋하고, `@AfterEach`에서 직접 지웁니다.
- 테스트가 실제로 경합을 일으키는지도 봤습니다. 팔로우 SQL에서 `on conflict`를 잠시 빼고 돌리면 8개 중 7개가 500이었습니다.
- 로컬 서버에 curl 3개를 동시에 보내도 행은 하나였습니다.

### 하위 사례: 제보 하루 한도

제보는 사용자당 하루 10건입니다. "센 뒤 넣기"도 같은 틈이 있어서, 사용자 행을 `PESSIMISTIC_WRITE`로 잠근 뒤 셉니다(Hibernate가 `SELECT … FOR NO KEY UPDATE`를 냅니다).

- **처음 테스트의 문제**: "0건에서 14개를 동시에 보내면 10개 성공, 4개 429" 테스트는 **잠금을 빼도 통과했습니다**. 커넥션을 얻은 트랜잭션들이 같은 개수를 보고 함께 넣는데, 커넥션 풀 크기(10)가 한도(10)와 같아 0건에서 시작하면 잠금이 없어도 정확히 10개가 통과했습니다.
- **고친 테스트**: `ConcurrentSuggestionLimitTest.concurrentSuggestionsAtTheLimitLetOnlyOneThrough`는 9건을 미리 넣고 20개를 동시에 보냅니다. 기대는 201 하나, 429 열아홉입니다.
- **보호 장치를 빼고 확인했습니다**:
  - 잠금을 빼면 세 번 모두 20개 중 10개가 통과해 실패했습니다.
  - 10은 커넥션 풀 크기(Hikari 기본값)입니다. 잠금을 뺀 채 풀 크기를 5·10·15로 바꿔 각각 세 번 돌리자, 통과 수가 매번 정확히 5·10·15였습니다. 동시에 커넥션을 얻은 트랜잭션이 모두 "9건"을 보고 함께 넣은 것입니다.
  - 처음 테스트 조건(0건에서 14개 동시)을 잠금 없이 풀 크기 15로 세 번 돌리면, 매번 14개가 모두 통과했습니다. 풀 크기가 한도와 같아 결과가 우연히 맞았던 것입니다.
  - 잠금을 되돌리면 세 크기 모두 1개만 통과합니다.

### 한계

- 행 잠금은 한 사용자의 제보 요청만 줄 세웁니다. 다른 사용자에게는 영향이 없지만, 같은 사용자의 동시 제보는 잠금을 기다립니다.
- 경합 테스트는 실제 스레드 타이밍에 기댑니다. 그래서 "보호 장치를 빼면 실패한다"를 한 번 확인한 뒤에야 의미가 있습니다.

---

## 2. 둘러보기 목록이 인덱스를 쓰지 않음

### 상황

둘러보기는 공개·휴업 공간을 최근 공개순으로 24개씩 보여 줍니다(다음 페이지가 있는지 보려고 25행을 읽습니다). 처음 만든 인덱스 `places(status, published_at desc, id desc)`가 쓰이지 않았고, PostgreSQL은 조건에 맞는 행을 모두 읽은 뒤 정렬했습니다.

### 원인

- **인덱스**: 조건이 `status in ('published','paused')` 두 값입니다. 이 인덱스에서는 두 상태가 따로 정렬되어 있어서, 합친 `published_at desc` 순서를 바로 꺼낼 수 없습니다.
- **커서**: 다음 페이지 조건을 `published_at < ? or (published_at = ? and id < ?)`로 썼습니다. PostgreSQL은 이 `or` 조건을 인덱스 범위 조건으로 쓰지 못하고 필터로 겁니다. 그래서 인덱스를 처음부터 읽으며 커서 앞의 행을 모두 버렸습니다.

### 해결

- 목록 대상만 담는 **부분 인덱스**로 바꿨습니다(마이그레이션 V3).

  ```sql
  create index idx_places_listed_published on places (published_at desc, id desc)
    where status in ('published', 'paused') and slug is not null;
  ```

- 커서 조건을 **행 비교**로 바꿨습니다(`PlaceRepository.findTilesAfter`). 인덱스의 두 열과 같은 순서라 범위 조건이 되어, 커서 위치에서 바로 읽기 시작합니다. Hibernate 7은 이 JPQL을 SQL `(p1_0.published_at,p1_0.id)<(?,?)`로 그대로 내보냅니다(`show-sql`로 확인).

  ```
  and (p.publishedAt, p.id) < (:at, :id)
  ```

### 확인

[scripts/explain-browse.sh](../scripts/explain-browse.sh)로 재현합니다.
1. 임시 `postgres:18` 컨테이너에 V1·V2를 적용합니다.
2. 공간 2만 곳(목록 대상 1만 6천)을 넣고 옛 인덱스로 잽니다.
3. V3를 적용하고 다시 잽니다.

같은 `EXPLAIN ANALYZE`를 세 번 돌려 마지막 값을 씁니다.

| 경우 | 전: 옛 인덱스 | 후: 부분 인덱스 |
| --- | --- | --- |
| 첫 페이지 | 5.337 ms (Seq Scan + 정렬) | 0.043 ms (Index Scan, 25행) |
| 8,000번째 뒤 페이지, `or` 조건 | 3.303 ms (Seq Scan + 정렬) | 0.855 ms (Index Scan, 필터로 8,000행 버림) |
| 8,000번째 뒤 페이지, 행 비교 | 3.497 ms (Seq Scan + 정렬) | 0.045 ms (Index Scan, 25행) |

시간은 기계마다 다릅니다. 봐야 할 것은 실행 계획의 모양입니다. 전에는 대상 행을 모두 읽고 정렬했고, 후에는 인덱스 순서대로 25행만 읽고 멈춥니다. 목록 API가 쿼리 한 번으로 끝나는 것은 `PlacePublicApiTest.browseRunsOneQuery`가 고정합니다.

### 한계

- **지금 운영 데이터는 수십 행이라 체감 차이가 없습니다.** 바뀐 것은 행 수에 비례하던 비용이 페이지 크기에 비례하게 된 것입니다.
- 인덱스 조건과 쿼리 조건이 같아야 쓰입니다. 목록에 나오는 상태가 바뀌면 인덱스도 마이그레이션으로 함께 바꿔야 합니다.
- 상태가 바인드 파라미터라, PostgreSQL이 generic 계획을 고르면 부분 인덱스를 못 씁니다. 기본 설정(`plan_cache_mode = auto`)에서는 custom 계획이 유지되는 것을 확인했습니다. 준비된 문장을 8번 실행한 뒤에도 generic 0회·custom 9회였고 0.008 ms였습니다. `force_generic_plan`으로 강제하면 Seq Scan + 정렬, 3.209 ms였습니다.

---

## 3. 프록시 뒤 OAuth 리디렉트

### 상황

브라우저는 Next 서버를 거쳐 백엔드를 부릅니다(같은 출처 프록시). 로그인 테스트는 모두 통과했습니다. 그런데 compose로 띄우고 프론트(3000)를 거쳐 구글 취소 콜백을 curl로 흉내 내자, 응답 `Location`이 `/places/x?login=error`가 아니라 `http://backend:8080/places/x?login=error`였습니다. 브라우저였다면 컨테이너 내부 주소로 이동했을 것입니다.

### 원인

- 코드는 상대 경로로 리디렉트했지만, Tomcat의 `sendRedirect`가 Host 헤더로 절대 주소를 만듭니다.
- Next rewrites 프록시는 Host를 목적지(백엔드) 주소로 바꿔 보냅니다. 덧붙이는 헤더는 `X-Forwarded-Host` 하나이고 `X-Forwarded-Proto`는 없습니다(Next 16.3.8 소스 확인).
- MockMvc는 Tomcat을 거치지 않아 테스트로는 드러나지 않았습니다.
- 같은 이유로, 구글에 보낼 `redirect_uri`를 요청에서 만들면 백엔드 주소가 됩니다.

### 해결

- `server.tomcat.use-relative-redirects: true`로 상대 경로를 그대로 보냅니다(Boot 4.1.1 기본값은 false).
- 구글 `redirect_uri`는 요청에서 만들지 않고 `FRONTEND_URL/login/oauth2/code/google`로 고정합니다. Spring Security 7.1.1은 토큰 요청에 처음 저장한 `redirect_uri`를 그대로 쓰고 콜백 URL과 비교하지 않아서 고정값으로 충분합니다(jar 확인).
- `X-Forwarded-*` 헤더는 개발에서 믿지 않습니다. 로컬에서는 8080이 열려 있어 누구나 위조할 수 있기 때문입니다. 운영(`prod`)에서만 `forward-headers-strategy: native`를 켭니다. 운영에서는 Caddy만 Tomcat에 닿고, Caddy가 들어온 헤더를 자기 값으로 덮어씁니다.
- 로그인 뒤 돌아갈 경로는 `/`로 시작하는 사이트 안 경로만 받습니다. `//`, 역슬래시, 제어 문자가 있으면 `/`로 바꿉니다(오픈 리디렉트 방지, `ReturnPath`).

### 확인

- **로컬**:
  - 같은 curl의 응답이 `location: /places/x?login=error`로 바뀌었습니다.
  - curl로 구글에 보내는 주소에 `redirect_uri=http://localhost:3000/login/oauth2/code/google`이 들어가고, 구글이 오류 없이 로그인 화면으로 넘기는 것을 봤습니다.
  - `redirect=/%09/evil.example`로 시작한 로그인이 `/`로 돌아왔습니다.
- **테스트**:
  - `ReturnPathTest`가 `//evil`, `/\evil`, 탭·줄바꿈이 섞인 경로, `javascript:` 등을 확인합니다.
  - `ProdProfileTest.trustsForwardedHeadersFromProxy`가 운영 프로필에서만 `native`인 것을 고정합니다.
- **운영**:
  - 구글로 가는 주소에 `redirect_uri=https://jieuni.space/login/oauth2/code/google`이 들어갑니다.
  - 로그인 뒤 `SESSION`(HttpOnly·Secure·Lax)과 `XSRF-TOKEN`(Secure)이 Vercel → Caddy → Tomcat 경로에서도 붙는 것을 확인했습니다.
  - 확인 명령은 [RUNBOOK.md 11절](RUNBOOK.md#11-운영-확인)에 있습니다.

### 한계

- 상대 경로 리디렉트는 Tomcat을 띄워야 드러나는 동작이라, MockMvc 테스트로는 고정하지 못했습니다. 프록시나 서버 설정을 바꾸면 compose와 curl로 다시 확인해야 합니다.
- "토큰 요청이 콜백 URL과 비교하지 않는다"는 Spring Security 7.1.1의 동작에 기댑니다. 메이저 버전을 올리면 다시 확인해야 합니다.
- Vercel 프리뷰 배포에서 로그인하면 운영 주소(`FRONTEND_URL`)로 돌아갑니다. 프리뷰에서는 로그인 뒤 화면을 확인할 수 없습니다.
