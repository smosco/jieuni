# 기술 결정

이 문서는 기술 결정마다 무엇을 골랐고, 왜 골랐고, 무엇을 포기했는지 적습니다. "왜 이렇게 만들었나"가 궁금할 때 읽습니다. 구현하며 부딪힌 문제와 확인 과정은 [TROUBLESHOOTING.md](TROUBLESHOOTING.md), 아직 만들지 않은 기능은 [SPEC.md 11절](SPEC.md#11-공개-뒤-계획)에 있습니다.

## 전제

- **범위**: 공개 화면(피드, 둘러보기, 공간 상세, 만든 사람 프로필), 로그인 뒤 기능(저장, 팔로우, 제보), 관리자 화면(등록 폼, 편성, 제보 검토).
- **원칙**: 저장소의 모든 줄을 설명할 수 있어야 합니다. 설명할 수 없는 복잡도는 넣지 않습니다.

## 요약

| 레이어 | 채택 |
| --- | --- |
| 프론트엔드 | Next.js 16 (App Router) + TypeScript, CSS Modules, TanStack Query, Zod |
| 백엔드 | Spring Boot 4.1 (Java 25) REST API |
| 데이터 접근 | Spring Data JPA + 공개 목록은 DTO 프로젝션 쿼리 |
| DB | PostgreSQL 18 + Flyway |
| 인증·권한 | Spring Security OAuth2 로그인(구글) + Spring Session JDBC, user/admin 역할 |
| 콘텐츠 | DB + 관리자 등록 폼, 공개할 때 검증 |
| 사진 | Cloudflare R2(개발·운영 버킷 분리), 서명된 URL로 브라우저가 직접 업로드 |
| 예약 공개 | 조회 시점에 계산 + 공개 시각에 홈 캐시를 비우는 스케줄러 |
| 배포 | 프론트 Vercel, 백엔드·DB는 EC2 한 대에 Docker Compose + Caddy, 도메인 `jieuni.space` |
| CI | GitHub Actions: 프론트 타입 검사·lint·Vitest, 백엔드 통합 테스트, 백엔드 이미지 빌드 |

버전과 확인한 공식 문서는 [부록](#부록-버전과-출처)에 있습니다.

---

## 1. 백엔드 — Spring Boot

| 선택지 | 장점 | 단점 |
| --- | --- | --- |
| Supabase만 | 서버 코드가 거의 없고 로그인·권한·DB가 한 번에 됩니다. 무료 | REST API 설계, 서버 권한 검사, 트랜잭션, 쿼리 튜닝을 보여 줄 코드가 남지 않습니다. 권한이 SQL 정책(RLS) 안에 숨습니다. 무료 프로젝트는 7일 요청이 없으면 멈춥니다 |
| **Spring Boot** | 트랜잭션, 서버 권한 검사, 스케줄링, 세션 저장을 표준 방식(Spring Data·Security·Session)으로 다룹니다. 권한 검사가 SQL 정책(RLS) 안에 숨지 않고 애플리케이션 코드와 테스트에 드러납니다 | 서버를 늘 띄워야 해서 비용과 운영 부담이 생깁니다 |

- **채택**: Spring Boot 4.1, Java 25(LTS). 3.5는 오픈소스 지원이 2026-06-30에 끝났고 4.0도 2026-12에 끝나서 4.1로 시작했습니다.
- **포기한 것**: Supabase의 무료·무운영, 프론트와 같은 언어(TypeScript)로 끝나는 단순함.

## 2. 데이터 접근 — JPA + DTO 프로젝션

| 선택지 | 장점 | 단점 |
| --- | --- | --- |
| JdbcTemplate | 실행되는 SQL이 코드에 그대로 보입니다 | 관리자 CRUD(공간과 자식 목록, 만든 사람, 편성)마다 매핑·INSERT·UPDATE 코드를 손으로 반복해 씁니다 |
| **Spring Data JPA** | 관리자 CRUD(공간, 크레딧, 사진, 출처, 편성)의 반복 코드가 줄어듭니다 | 암묵적 동작이 많습니다. 목록 화면이 많아 N+1이 생기기 쉽습니다 |

- **채택**: JPA. 다만 공개 목록(피드, 둘러보기, 프로필 격자, 내 공간)은 엔티티 대신 **DTO 프로젝션 쿼리**로 씁니다. 목록 API마다 실행되는 쿼리 수를 Hibernate `Statistics`로 세어 테스트에 고정했습니다.
- `open-in-view`는 끕니다. 트랜잭션은 서비스 계층에서만 엽니다.
- `Place`의 자식 목록(크레딧·출처·사진·임베드)은 둘 이상을 `join fetch`하지 않습니다(카테시안 곱). 대신 `default_batch_fetch_size: 100`으로 목록마다 IN 쿼리 한 번씩 읽습니다.
- **둘러보기 목록**은 부분 인덱스 `(published_at desc, id desc) where status in ('published','paused') and slug is not null`과 행 비교 커서 `(published_at, id) < (?, ?)`를 씁니다. 2만 행 합성 데이터에서 첫 페이지가 5.337 → 0.043ms가 됐습니다([scripts/explain-browse.sh](../scripts/explain-browse.sh)로 재현, 시간은 기계마다 다릅니다). 지금 운영 데이터는 수십 행이라 체감 차이는 없습니다. 경위는 [TROUBLESHOOTING.md](TROUBLESHOOTING.md#2-둘러보기-목록이-인덱스를-쓰지-않음).
- **포기한 것**: SQL이 전부 눈에 보이는 투명함. 목록 상태 값이 바뀌면 부분 인덱스 조건도 마이그레이션으로 함께 바꿔야 합니다.

## 3. DB — PostgreSQL 18 + Flyway

- 이 프로젝트가 기대는 기능을 PostgreSQL이 그대로 제공합니다: `INSERT … ON CONFLICT`(저장·팔로우 upsert), 부분 인덱스(둘러보기 목록), `CHECK` 제약(enum, 평일 편성), `DEFERRABLE` 유일 제약(사진 순서 바꾸기).
- 스키마는 Flyway 마이그레이션으로만 바꿉니다. JPA `ddl-auto`는 `validate`입니다.
- **필수 항목은 DB 제약이 아니라 공개 전환 때 서비스 계층에서 검사**합니다. 제보·작성 중 상태에서는 빈칸이 정상이기 때문입니다. 형식(길이, enum, unique)은 DB 제약으로 둡니다. enum은 `varchar` + `CHECK`입니다.
- 공개 여부와 상관없이 늘 참이어야 하는 규칙은 DB `CHECK`로도 둡니다. 예: 확인된 크레딧에는 출처가 있다, 공개된 공간에는 `published_at`이 있다, 편성은 평일만.
- `place_photos`에는 `UNIQUE (place_id, position) DEFERRABLE INITIALLY DEFERRED`를 겁니다. position 0이 대표 사진이라 같은 position이 둘이면 대표가 정해지지 않습니다. 순서를 맞바꾸는 UPDATE 중간에 잠깐 겹치므로 커밋 때 검사합니다.
- 탈퇴 규칙은 FK 동작으로도 겁니다. `saves`·`follows`는 `ON DELETE CASCADE`, `places.suggested_by`·`credit_suggestions.user_id`는 `ON DELETE SET NULL`입니다.
- `created_at`·`updated_at`은 JPA Auditing으로 채우되 `Clock` 빈에서 시각을 읽습니다. 테스트에서 시계를 고정할 수 있습니다.
- **동시에 들어오는 같은 요청**:
  - 저장·팔로우는 PostgreSQL 네이티브 upsert(`INSERT … ON CONFLICT`)로만 씁니다. 엔티티는 읽기용입니다.
  - 제보 하루 한도는 사용자 행을 `PESSIMISTIC_WRITE`로 잠근 뒤 셉니다. 한 사용자의 요청만 줄 세우므로 다른 사용자에게는 영향이 없습니다.
  - 경합을 재현한 테스트는 [TROUBLESHOOTING.md](TROUBLESHOOTING.md#1-동시-요청에서-중복-키-오류와-한도-초과)에 있습니다.
- id를 직접 넣는 `DailyPick`은 `Persistable`을 구현해 `save()`가 SELECT 없이 INSERT합니다.
- 개발용 시드는 `dev` 프로필에서만 등록되는 `ApplicationRunner`가 넣습니다([SPEC.md 10절](SPEC.md#10-개발용-시드)).

**포기한 것**: 관리형 DB의 자동 백업. 매일 직접 백업합니다(9절).

## 4. 인증·권한 — OAuth2 로그인 + 서버 세션

| 선택지 | 장점 | 단점 |
| --- | --- | --- |
| Supabase Auth | 로그인이 바로 됩니다 | 1절에서 Supabase를 빼면 이것만 따로 쓸 이유가 약합니다 |
| OAuth2 + JWT(프론트가 토큰 보관) | 서버가 상태를 갖지 않습니다 | 토큰이 브라우저 JS에 노출되고, 로그아웃·탈퇴·권한 회수를 바로 반영하기 어렵습니다 |
| **OAuth2 + 서버 세션(HttpOnly 쿠키)** | 토큰을 JS에 두지 않습니다. 로그아웃·탈퇴가 바로 반영됩니다 | 요청마다 세션을 읽어야 합니다 |

- **채택**: 서버 세션. Spring Session JDBC로 PostgreSQL에 저장해 재배포 뒤에도 유지되고, 서버를 늘려도 세션을 공유합니다. 세션은 마지막 요청부터 30일 유지됩니다.
- **제공자**: 구글만. 카카오는 개인 개발자가 이메일을 받으려면 비즈 앱 전환이 필요해 공개 뒤로 미뤘습니다. 관리자도 구글 계정이고, `ADMIN_EMAILS`로 정합니다.
- **역할**: `user`, `admin`. `/api/admin/**`은 admin만(403). 역할은 세션이 아니라 요청마다 DB의 `users.role`로 확인합니다. DB의 역할은 서버가 시작할 때와 로그인할 때 `ADMIN_EMAILS`에 맞춥니다. 그래서 `ADMIN_EMAILS`에서 빼고 서버를 다시 띄우면, 다시 로그인하지 않아도 다음 요청부터 막힙니다. 남의 저장·팔로우는 처음부터 본인 것만 조회하는 쿼리로 막습니다.
- **CSRF**: 켭니다. Spring Security 7의 `csrf.spa()`(쿠키 저장소 + 평문 토큰)를 쓰고, GET 응답에도 쿠키가 실리도록 토큰을 한 번 읽는 필터(`CsrfCookieFilter`)를 `CsrfFilter` 뒤에 둡니다.
- **구글 토큰은 저장하지 않습니다.** `oauth2Login`의 기본 저장소는 받은 액세스 토큰을 프로세스 메모리에 쌓습니다. 구글 API를 부르지 않으므로 아무것도 저장하지 않는 `NoStoreAuthorizedClientRepository`를 넣었습니다. 쓰지 않는 토큰은 서버가 뚫렸을 때 노출될 정보만 늘립니다.
- **포기한 것**: 무상태 서버. 요청마다 세션을 DB에서 읽고, 만료된 세션 행은 Spring Session의 정리 작업이 기본 일정(1분마다)으로 지웁니다.

## 5. 콘텐츠 — DB + 관리자 폼

| 선택지 | 장점 | 단점 |
| --- | --- | --- |
| 저장소의 MDX 파일 | 관리자 화면이 필요 없습니다. 모든 수정이 git에 남습니다. 서버가 죽어도 페이지가 보입니다 | 화면도 API도 DB도 없어 풀스택으로 보여 줄 것이 남지 않습니다. 사용자 제보를 콘텐츠로 이어 붙이기 어렵습니다 |
| **DB + 관리자 등록 폼** | 폼 하나에 CRUD, 양쪽 검증, 파일 업로드, 역할 권한, 상태 흐름, 캐시 갱신이 모두 들어갑니다. 제보가 같은 테이블의 `suggested` 상태로 이어집니다 | 관리자 화면을 만들어야 합니다. 콘텐츠 변경 이력이 git에 남지 않습니다 |

- **채택**: DB + 관리자 폼. 공개 페이지는 캐시하고, 수정·공개할 때 그 페이지의 캐시만 지웁니다(6절).
- **포기한 것**: git으로 남는 콘텐츠 이력.

## 6. 프론트엔드 — Next.js 16 App Router + TypeScript

**같은 출처 프록시.** 브라우저는 백엔드를 직접 부르지 않습니다. `next.config.ts`의 `rewrites`가 `/api/:path*`·`/oauth2/:path*`·`/login/oauth2/:path*`를 `API_URL`로 넘기고, 클라이언트는 상대 경로만 씁니다.

- 이유: 출처가 다르면 credentials를 허용하는 CORS를 열어야 하고, API 도메인이 심은 CSRF 쿠키를 프론트 JS가 읽으려면 쿠키 도메인을 넓혀야 합니다. 같은 출처로 두면 둘 다 필요 없습니다.
- rewrites 목적지는 `next build` 때 결과물에 굳습니다. 그래서 `API_URL`은 실행 환경 변수이면서 빌드 인자이기도 합니다(Dockerfile `ARG API_URL`).
- 재생성 라우트 `app/api/revalidate/route.ts`는 파일 라우트라 rewrites보다 먼저 잡혀 백엔드로 넘어가지 않습니다.
- **포기한 것**: API 응답이 Next 서버를 한 번 더 거칩니다(Vercel에서는 함수 호출 하나).

**공개 페이지 렌더링과 캐시.**

- `/`, `/places`, `/makers`는 요청마다 렌더링(`revalidate = 0`)하고, 백엔드 응답은 데이터 캐시(`fetch`의 `next: { tags, revalidate: 3600 }`)에 둡니다. 정적 라우트로 두면 빌드 때 백엔드가 없어 빌드가 깨지거나 빈 화면이 굳습니다. 이 조합은 [Next 문서](https://nextjs.org/docs/app/guides/caching-without-cache-components)가 보장합니다.
- `/places/[slug]`, `/makers/[slug]`는 빈 `generateStaticParams` + `dynamicParams = true`입니다. 첫 요청 때 만들어 캐시합니다. 그래서 빌드는 백엔드 없이 통과합니다.
- Spring이 공간을 공개·수정하면 프론트의 재생성 라우트를 부르고, 라우트는 `revalidateTag(tag, { expire: 0 })`로 캐시를 지웁니다. Next 16의 다른 프로필(`'max'` 등)은 다음 요청에 옛 페이지를 먼저 보여 주므로, "공개한 뒤 새로고침 한 번에 보인다"를 지키려고 `expire: 0`을 씁니다. `updateTag`는 Server Action 전용이라 외부 호출 라우트에서 쓸 수 없습니다.
- `cacheComponents`는 켜지 않습니다. 켜면 빈 `generateStaticParams`가 빌드 오류가 됩니다.

**재생성 호출.**

- 호출은 트랜잭션 커밋 뒤(`AFTER_COMMIT`)에 관리자 요청 스레드에서 한 번만 보냅니다(연결 2초, 읽기 5초). 실패는 로그만 남깁니다.
- 재시도·큐·`@Async`는 두지 않습니다. 데이터 캐시가 1시간마다 새로 받으므로 안전망이 있고, 재시도는 관리자 응답만 늦춥니다. 대신 커밋 뒤 HTTP 호출 동안(최대 7초) DB 연결을 붙잡습니다. 관리자 경로라 허용합니다.
- 호출 주소(`REVALIDATE_URL`)는 공개 주소(`FRONTEND_URL`)와 나눕니다. `FRONTEND_URL`은 구글 `redirect_uri`라 브라우저가 보는 주소여야 하는데, compose 안의 백엔드에서 `localhost:3000`은 자기 자신입니다. 비우면 `FRONTEND_URL`을 쓰고, 이 선택은 코드(`RevalidateClient.url`)가 합니다. 빈 문자열도 "없음"으로 봐야 해서 설정 파일의 기본값 문법에 맡기지 않습니다.
- JDK `HttpClient`는 HTTP/1.1로 고정합니다. 기본값은 `http://`에도 h2c 업그레이드 헤더를 붙이는데, Next 서버가 그 요청을 응답 없이 끊었습니다.

**로그인 뒤 화면과 관리자 화면.**

- 클라이언트 컴포넌트에서 API를 부르고, 서버 상태는 TanStack Query로만 다룹니다. 공개 화면 위의 사용자별 값(저장·팔로우 상태)은 `/api/me/state`로 따로 받습니다.
- 저장·팔로우는 낙관적 업데이트입니다. 누르는 즉시 캐시를 바꾸고, 실패하면 되돌리고 토스트를 띄웁니다.
- 관리자 폼 상태는 라이브러리 없이 `useState`로 둡니다. 값 객체 하나와 서버 `fieldErrors`와 같은 키의 `errors`뿐이고 검증은 Zod가 합니다. 서버 자동 저장은 하지 않습니다. `PATCH`가 스칼라 필드 전부를 덮어써서, 자동 저장이면 공개된 공간을 고치는 중간에 400(필수 칸 빔)이나 409(slug 중복)가 납니다. 대신 저장하지 않은 입력은 브라우저(`localStorage`)에 공간마다 보관합니다.
- **포기한 것**: 폼 라이브러리의 dirty 추적·필드 배열 도우미.

**스타일.** CSS Modules + CSS 변수(`frontend/app/tokens.css`)만 씁니다. Tailwind와 UI 라이브러리는 쓰지 않습니다. 피드 세로 넘기기와 사진 가로 넘기기는 CSS scroll-snap으로 만듭니다. **포기한 것**: 라이브러리로 얻는 속도.

**에세이 마크다운.** `react-markdown`을 서버 컴포넌트에서만 써서 클라이언트 번들에 넣지 않습니다. 허용 요소는 h1~h3·p·ul·ol·li·strong·em·a·blockquote(h1은 h2로 그림)이고, `skipHtml`로 raw HTML을 버립니다. 링크는 새 창 + `noopener noreferrer`이고, 위험한 주소는 기본 `urlTransform`이 비웁니다. **포기한 것**: 의존성 없는 직접 파서. 링크·인용·이스케이프를 손으로 맞추는 쪽이 더 위험하다고 봤습니다.

**CSP는 nonce 없이.** `next.config.ts`의 `headers()`로 모든 경로에 CSP · `Referrer-Policy` · `X-Content-Type-Options` · `Permissions-Policy`를 답니다.

- `script-src`는 `'self' 'unsafe-inline'`입니다(개발 서버만 `'unsafe-eval'`). nonce를 쓰면 모든 페이지를 요청마다 그려야 해서 태그 캐시·상세 페이지 캐시와 맞지 않습니다([Next CSP 안내](https://nextjs.org/docs/app/guides/content-security-policy)).
- `img-src`에는 사진 도메인(빌드 환경 변수 `PHOTO_PUBLIC_BASE_URL`), `connect-src`에는 R2 업로드 주소, `frame-src`에는 youtube-nocookie·instagram을 둡니다. 헤더는 빌드 결과물에 굳으므로 사진 주소가 바뀌면 다시 빌드합니다.
- Zod 4는 `new Function`을 시도해 CSP 위반 보고를 남기므로 `z.config({ jitless: true })`로 끕니다.
- **포기한 것**: 인라인 스크립트 주입을 CSP로 막는 것. XSS 방어는 React 이스케이프와 마크다운 허용 목록에 기댑니다.

**관리자 화면의 작업용 숫자.** "숫자 표시 금지"([SPEC.md 1절](SPEC.md#1-지키는-규칙))는 공개 화면과 공개 API가 대상입니다. 관리자 화면의 만든 사람별 공간 수(`placeCount`)와 검토 대기 건수는 예외입니다. 인기나 순위가 아니라 같은 사람을 고르고 할 일을 보게 돕는 값이고, 관리자만 봅니다.

## 7. 사진 — R2 + 서명된 URL 직접 업로드

| 선택지 | 장점 | 단점 |
| --- | --- | --- |
| 서버가 파일을 받아 저장소로 전달 | 구현이 단순합니다 | 서버 메모리·대역폭을 쓰고, 큰 파일에서 느립니다 |
| **서버는 서명된 URL만 발급, 브라우저가 저장소에 직접 업로드** | 파일이 서버를 거치지 않습니다 | 업로드한 뒤 등록이 빠진 파일을 정리해야 합니다 |

- **채택**: 서명된 URL 직접 업로드. URL은 관리자만, 5분짜리로 받습니다. `Content-Type`과 파일 크기를 서명에 넣어 다른 형식·크기로는 올라가지 않습니다. 등록할 때 서버가 두 파일이 있는지 확인하고, 등록되지 않은 채 24시간이 지난 파일은 매일 03:00(KST) 정리 작업이 지웁니다.
- **저장소는 R2**입니다. 10GB까지 무료이고 내보내기 트래픽이 무료입니다. S3 호환이라 AWS SDK를 그대로 씁니다. 개발과 운영 모두 R2이고 버킷만 다릅니다. 로컬 S3 호환 서버(MinIO)는 커뮤니티 저장소가 보관 처리되고 Docker Hub 이미지가 사라져 쓰지 않았습니다. 그 대가로 개발 환경이 완전히 오프라인은 아닙니다. 테스트는 저장소 없이 가짜 구현으로 합니다.
- **변환은 브라우저에서** WebP 두 크기(1600px, 800px)로 합니다. 서버에 이미지 처리 라이브러리를 두지 않고, Vercel 무료 플랜의 이미지 변환 한도(월 5,000회, 원격 이미지 포함)도 쓰지 않기 위해서입니다. 서버는 `PHOTO_PUBLIC_BASE_URL + objectKey`로 완성한 두 URL을 주고, 프론트는 `<img srcset>`으로 그립니다. Next 이미지 최적화는 쓰지 않습니다.
- 사진 출처 규칙은 [SPEC.md 8절](SPEC.md#사진)에 있습니다.
- **포기한 것**: 서버 쪽 이미지 처리의 일관성. 브라우저마다 인코딩 품질이 조금 다를 수 있습니다.

## 8. 예약 공개 — 하루 한 곳 피드

| 선택지 | 장점 | 단점 |
| --- | --- | --- |
| 스케줄러가 매일 08:00에 "공개" 플래그를 켠다 | 직관적입니다 | 스케줄러가 한 번 실패하면 그날 공간이 열리지 않습니다 |
| **공개 여부를 조회 시점에 계산** (`pick_date 08:00 KST <= 지금`) | 스케줄러가 없어도 정확합니다. 시계를 고정해 경계를 테스트할 수 있습니다 | 홈 페이지가 캐시라 캐시를 따로 비워야 합니다 |

- **채택**: 조회 시점 계산 + 캐시를 비우는 스케줄러. 스케줄러는 월~금 08:00:05와 매일 00:00:05(KST)에 홈 캐시를 비웁니다. 자정 것은 피드 마지막 화면의 "내일"이 "오늘"로 바뀌기 때문입니다. 정각보다 5초 늦추는 것은, 직전 재생성이 오늘 칸 없는 피드를 다시 캐시하는 일을 피하려는 것입니다. 스케줄러가 실패해도 데이터 캐시가 1시간 안에 새로 받습니다.
- 시각은 DB와 서버 모두 UTC로 저장하고, 08:00 KST 경계 계산만 `Asia/Seoul`로 합니다. 시간은 `Clock` 빈으로만 읽습니다.
- 공휴일은 다루지 않습니다. 편성은 평일만 따지고, 공휴일 칸을 비우면 피드에 그날 공간이 없을 뿐입니다.
- **포기한 것**: 스케줄러 하나로 끝나는 단순함.

## 9. 배포 — Vercel + EC2 한 대 + R2

**채택**

- **프론트**: Vercel Hobby, 함수 리전 `icn1`(서울). Hobby는 함수 리전을 하나만 고를 수 있고 비상업 용도만 허용합니다. 백엔드가 서울이라 rewrites 프록시 왕복이 짧습니다. Node 버전은 `frontend/package.json`의 `engines.node`로 정해 콘솔 설정에 의존하지 않습니다.
- **백엔드 + PostgreSQL**: AWS EC2 한 대(서울), docker compose로 `db` · `backend` · `caddy`를 띄웁니다. 8080과 5432는 밖에 열지 않고 Caddy의 80·443만 엽니다.
- **이미지**: CI가 만들어 GHCR에 올리고 서버는 받기만 합니다. 서버가 ARM(t4g)이라 GitHub Actions의 ARM 러너에서 QEMU 없이 빌드합니다. `main`에 `backend/**`가 머지되면 `<짧은 sha>`와 `latest` 태그를 붙입니다. 패키지는 공개라 서버가 로그인 없이 받습니다. 비밀값은 이미지에 없고 서버 `.env`에만 있습니다.
- **프록시 헤더**: `server.forward-headers-strategy: native`는 `prod`에서만 켭니다. 운영에서는 같은 docker 네트워크의 Caddy만 Tomcat에 닿고, Caddy가 들어온 `X-Forwarded-*`를 버리고 자기 값으로 덮어씁니다. 그래서 그 헤더를 믿을 수 있고, `X-Forwarded-Proto: https`로 CSRF 쿠키에도 `Secure`가 붙습니다. 모든 헤더를 믿는 `framework` 방식은 쓰지 않습니다. 구글 `redirect_uri`는 `FRONTEND_URL`로 고정해 이 설정과 무관합니다([TROUBLESHOOTING.md 3](TROUBLESHOOTING.md#3-프록시-뒤-oauth-리디렉트)).
- **사진**: R2 운영 버킷(개발 버킷과 분리). 공개 읽기는 커스텀 도메인 `photos.jieuni.space`입니다(r2.dev 주소는 속도 제한이 있는 개발용이라 끕니다).
- **DB 백업**: 서버 cron이 매일 04:00(KST) `deploy/backup.sh`를 부릅니다.
  - `db` 컨테이너의 `pg_dump -Fc` 결과를 rclone 공식 이미지(compose `backup` 서비스)가 비공개 버킷에 올립니다. 보관은 버킷 수명 규칙으로 7일입니다.
  - 호스트에 아무것도 설치하지 않습니다. 이 버킷만 쓰는 토큰을 따로 둡니다.
  - 세션 테이블은 구조만 덤프합니다. 세션 ID는 로그인 토큰이라 백업에 남기지 않습니다(복원하면 모두 로그아웃).
  - 탈퇴한 사용자의 데이터는 백업에 최대 7일 남습니다. 개인정보처리방침에 이 사실을 적었습니다.
- **도메인**: `jieuni.space`(Cloudflare에서 구입·DNS).
  - `jieuni.space` → Vercel.
  - `api.jieuni.space` → EC2 Elastic IP, Cloudflare 프록시는 끔. Caddy가 Let's Encrypt 인증서를 직접 받습니다. 브라우저는 이 주소를 부르지 않습니다(rewrites의 목적지).
  - `photos.jieuni.space` → R2 커스텀 도메인.
- **구글 운영 클라이언트는 개발과 따로** 둡니다. 운영 클라이언트에는 운영 리디렉트 URI 하나만 있어, 개발 설정을 바꿔도 운영 로그인에 영향이 없습니다.
- **프리뷰 배포**는 `API_URL`·`PHOTO_PUBLIC_BASE_URL`만 받아 운영 백엔드의 공개 GET만 읽습니다. 재생성 비밀은 넣지 않습니다. Vercel이 프리뷰에 `X-Robots-Tag: noindex`를 붙여 검색에 걸리지 않습니다.
- **글꼴**: `next/font/google`이 빌드 때 받아 자체 호스팅합니다. 다만 한글 글꼴의 선언 CSS가 커서 성능 목표를 못 맞추고 있어 다시 볼 대상입니다([SPEC.md 9절](SPEC.md#성능접근성)).

절차는 [RUNBOOK.md](RUNBOOK.md)에 있습니다.

**포기한 것**

- **도메인 전 임시 구성**(vercel.app 주소, 평문 HTTP, r2.dev): 두면 구글 리디렉트·R2 CORS·검색 등록을 두 번 해야 하고, Vercel ↔ EC2 구간이 잠시라도 평문이 됩니다.
- **Cloudflare 프록시 뒤의 API**: 켜면 인증서·실제 IP·`X-Forwarded-*`를 Cloudflare 기준으로 다시 맞춰야 합니다. 브라우저가 부르지 않는 주소라 얻는 것이 적습니다.
- **EC2에서 이미지 빌드**: 2GB 서버에서 Gradle 빌드는 빠듯하고 빌드 캐시가 서버에 남습니다.
- **비용 위험**: AWS Free Plan은 크레딧 최대 $200·최대 6개월이고, 끝나면 계정이 닫히고 90일 뒤 지워집니다([FAQ](https://aws.amazon.com/free/free-tier-faqs/)). 만료 전 유료 전환 날짜를 RUNBOOK에 둡니다. 서버가 하나라 장애 지점도 하나입니다.

## 10. CI · 테스트 · 협업

- **GitHub Actions**: PR과 `main` push마다, 바뀐 경로에 따라 ① 프론트 타입 검사·lint·Vitest ② 백엔드 `./gradlew test`(Testcontainers PostgreSQL)를 돌립니다. `main`에 `backend/**`가 머지되면 백엔드 이미지를 빌드합니다.
- **백엔드 통합 테스트**는 시드에 기대지 않고 필요한 데이터를 직접 만듭니다. 테스트로 고정한 것:
  - 공개 시 검증(필수 누락 400)과 상태 전환표 밖 409, 공개 후 slug 변경 409
  - 관리자 API 일반 사용자 403, CSRF 토큰 없는 POST 403
  - 운영 프로필에 개발용 로그인·시드·고정 시계가 없음
  - 저장 want↔been 전환과 해제, 다른 사용자 데이터 비노출, 탈퇴 시 데이터 처리
  - 팔로우한 사람의 공간: 중복 제거, "새로운" 표시와 탭 점
  - 피드 경계(수 07:59 / 08:00, 월 07:59 지난주, 금 08:00 뒤 다음 공개 시각)
  - 편성 규칙(주말 400, 중복 409, 열린 칸 409)
  - 목록 API 쿼리 수, 재생성 호출이 커밋 뒤에만 나감
  - 동시 요청(저장·팔로우 upsert, 제보 한도)
- **프론트**: 순수 함수(공개 검증 스키마, 피드 마지막 화면 문구, 저장 상태 전환 등)만 Vitest로 확인합니다. 화면은 [DESIGN.md](DESIGN.md)의 수치와 대조합니다.
- **협업 방식**: 혼자여도 기능 단위 브랜치 → PR → 병합으로 작업합니다. PR 템플릿에 무엇을, 왜, 어떻게 확인했는지 적습니다.

---

## 부록: 버전과 출처

2026-10에 공식 문서로 확인한 버전과, 결정에 쓴 사실의 출처입니다.

| 대상 | 버전 | 확인한 것 | 출처 |
| --- | --- | --- | --- |
| Spring Boot | **4.1.1** | OSS 지원: 3.5 → 2026-06-30 종료, 4.0 → 2026-12-31, 4.1 → 2027-07-31. Java 17~26 지원. 관리 버전: Spring Framework 7.0, Security 7.1, Session 4.1, Hibernate ORM 7.4.5, Flyway 12.4.0, Testcontainers 2.0.5 | [릴리스](https://github.com/spring-projects/spring-boot/releases) · [지원 기간](https://api.spring.io/projects/spring-boot/generations) · [시스템 요구사항](https://docs.spring.io/spring-boot/system-requirements.html) · [관리 좌표](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html) · [4.1 릴리스 노트](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.1-Release-Notes) |
| Boot 4 스타터 | — | `spring-boot-starter-data-jpa` · `spring-boot-starter-flyway`(4.0부터 필요) · `spring-boot-starter-session-jdbc` · `spring-boot-starter-security-oauth2-client` · `spring-boot-starter-validation` · `spring-boot-starter-actuator` · 테스트 `spring-boot-testcontainers`. Jackson 3는 `tools.jackson` | [4.0 마이그레이션 가이드](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) |
| Spring Session JDBC | 4.1 | 스키마 파일을 Flyway로 복사하고 `spring.session.jdbc.initialize-schema=never`. 만료 세션 정리 `cleanup-cron` 기본값 `0 * * * * *` | [Session JDBC](https://docs.spring.io/spring-session/reference/configuration/jdbc.html) |
| Spring Security | 7.1 | SPA용 CSRF는 `csrf.spa()`. 로그인·로그아웃 성공 시 CSRF 쿠키가 지워집니다. `spa()`는 토큰을 쓸 때만 쿠키를 만들어 GET 응답에는 쿠키가 없습니다(7.1.1 jar 확인) → `CsrfCookieFilter`. Spring Security Test의 `csrf()`는 저장소를 세션 기반으로 바꿔 끼우므로 테스트는 쿠키+헤더를 직접 싣습니다 | [CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) · [OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html) · [프록시 설정](https://docs.spring.io/spring-security/reference/features/exploits/http.html) |
| Testcontainers | 2.0.5 | 아티팩트 `org.testcontainers:testcontainers-postgresql`, `@ServiceConnection` | [Boot Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html) |
| Hibernate ORM | 7.4.5 | `Statistics.getPrepareStatementCount()`로 목록 API 쿼리 수 테스트 | [Statistics](https://docs.hibernate.org/orm/7.4/javadocs/org/hibernate/stat/Statistics.html) |
| PostgreSQL | **18** | 2030-11까지 지원. Docker `postgres:18`. Flyway 12.4가 18을 지원 | [버전 정책](https://www.postgresql.org/support/versioning/) · [Flyway PostgreSQL](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database) |
| Next.js | **16.3.8** | `revalidateTag(tag, profile)` 두 번째 인자 필수, `{ expire: 0 }`만 다음 요청에서 새로 만듭니다. `updateTag`는 Server Action 전용. `cacheComponents`를 켜면 빈 `generateStaticParams`가 빌드 오류. rewrites 프록시는 Host를 목적지로 바꾸고 `X-Forwarded-Proto`를 보내지 않습니다(16.3.8 소스 확인). `ImageResponse` 번들 500KB 제한 | [revalidateTag](https://nextjs.org/docs/app/api-reference/functions/revalidateTag) · [updateTag](https://nextjs.org/docs/app/api-reference/functions/updateTag) · [generateStaticParams](https://nextjs.org/docs/app/api-reference/functions/generate-static-params) · [cacheComponents](https://nextjs.org/docs/app/api-reference/config/next-config-js/cacheComponents) · [rewrites](https://nextjs.org/docs/app/api-reference/config/next-config-js/rewrites) · [ImageResponse](https://nextjs.org/docs/app/api-reference/functions/image-response) |
| Node.js | **24 LTS** | `engines.node: "24.x"`. Vercel은 `.nvmrc`가 아니라 이 값을 읽습니다 | [Node 릴리스](https://nodejs.org/en/about/previous-releases) · [Vercel Node 버전](https://vercel.com/docs/functions/runtimes/node-js/node-js-versions) |
| Vercel Hobby | — | 이미지 변환 월 5,000회(원격 이미지 포함, `<img>`는 제외), 함수 리전 하나, 비상업 전용 | [이미지 한도](https://vercel.com/docs/image-optimization/limits-and-pricing) · [Hobby](https://vercel.com/docs/plans/hobby) · [리전](https://vercel.com/docs/functions/configuring-functions/region) |
| Cloudflare R2 · AWS SDK for Java v2 | SDK **2.55.14** | 서명 URL은 PUT 지원, `*.r2.cloudflarestorage.com`에서만(커스텀 도메인 불가). `Content-Type`을 넣어 서명하면 브라우저가 같은 값을 보내야 합니다. 클라이언트는 R2 공식 Java 예제 설정(path style, 청크 전송 끔, 체크섬 `WHEN_REQUIRED`)을 따릅니다. 브라우저 PUT에는 CORS 정책 필요. 무료 10GB, 송신 무료 | [서명 URL](https://developers.cloudflare.com/r2/api/s3/presigned-urls/) · [Java SDK 예제](https://developers.cloudflare.com/r2/examples/aws/aws-sdk-java/) · [CORS](https://developers.cloudflare.com/r2/buckets/cors/) · [공개 버킷](https://developers.cloudflare.com/r2/buckets/public-buckets/) · [가격](https://developers.cloudflare.com/r2/pricing/) |
| MinIO | 쓰지 않음 | 커뮤니티 저장소 보관(archived), Docker Hub `minio/minio` 이미지 없음 | [github.com/minio/minio](https://github.com/minio/minio) |
| 구글 OAuth | — | scope `openid email profile`만 쓰면 검증 없이 게시 가능. 테스트 모드는 동의 7일 만료. 리디렉트 URI는 HTTPS·공개 도메인만 | [OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect) · [게시 상태](https://support.google.com/cloud/answer/15549945) · [URI 규칙](https://developers.google.com/identity/protocols/oauth2/web-server#uri-validation) |
| Caddy | 2 | `reverse_proxy`는 들어온 `X-Forwarded-*`를 버리고 자기 값으로 덮어씁니다 | [reverse_proxy](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy) |
| GHCR | — | 공개 패키지는 익명으로 받을 수 있고, 한 번 공개하면 비공개로 되돌릴 수 없습니다 | [가시성](https://docs.github.com/en/packages/learn-github-packages/configuring-a-packages-access-control-and-visibility) |
| rclone | 1.75 | 설정 파일 없이 `RCLONE_CONFIG_<리모트>_<옵션>` 환경 변수로 리모트를 만듭니다. 버킷 한정 R2 토큰은 `no_check_bucket` 필요 | [rclone](https://rclone.org/docs/#config-file) · [R2 rclone](https://developers.cloudflare.com/r2/examples/rclone/) |
