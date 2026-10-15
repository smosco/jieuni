# API

이 문서는 백엔드 HTTP API의 경로, 요청·응답, 상태 코드, 그리고 서비스 사이의 호출 계약을 적습니다. API 설계를 볼 때, 프론트나 백엔드에서 API를 바꾸기 전에 읽습니다. API 모양이 바뀌면 이 문서를 함께 고칩니다.

- 필드 이름은 [SPEC.md 3절](SPEC.md#3-용어)의 코드 이름을 따르고, JSON은 camelCase입니다.
- 시각은 ISO-8601 UTC(`2026-10-07T23:00:00Z`), 날짜는 `YYYY-MM-DD`입니다.

## 0. 공통

### 인증·보호

| 구분 | 규칙 |
| --- | --- |
| 세션 | Spring Session JDBC. 쿠키 `SESSION`, HttpOnly · SameSite=Lax · 운영 Secure. 유지 30일(마지막 요청 기준). 비로그인 요청은 세션을 만들지 않는다 |
| CSRF | 켬(Spring Security 7 `csrf.spa()`). 서버가 모든 응답에 `XSRF-TOKEN` 쿠키(HttpOnly 아님, 없을 때만 새로)를 주고, 프론트는 GET 외 요청에 `X-XSRF-TOKEN` 헤더로 돌려보낸다. 없으면 403. 로그인·로그아웃 성공 시 쿠키가 지워지므로 그 뒤 `GET /api/me`로 다시 받는다 |
| 같은 출처 | 브라우저는 Next를 거쳐 `/api/**`·`/oauth2/**`·`/login/oauth2/**`를 부른다. CORS 없음. 이유는 [DECISIONS.md 6절](DECISIONS.md#6-프론트엔드--nextjs-16-app-router--typescript) |
| 공개 | `GET` `/api/feed`, `/api/places/**`, `/api/makers/**`, `/api/sitemap`, `/actuator/health` |
| 로그인 필요 | `/api/me/**`, `/api/suggestions/**` → 비로그인 401 |
| 관리자 | `/api/admin/**` → 비로그인 401, `role=admin` 아니면 403. 역할은 세션이 아니라 요청마다 DB의 `users.role`로 본다. 서버가 시작할 때와 로그인할 때 `users.role`을 `ADMIN_EMAILS`에 맞춘다(목록에서 빼고 서버를 다시 띄우면, 다시 로그인하지 않아도 다음 요청부터 403) |
| 개발용 | `/api/dev/**` → `dev`·`test` 프로필에서만 빈이 존재 |

### 오류 본문

```json
{ "message": "사람이 읽는 한 문장" }
{ "message": "공개할 수 없어요", "fieldErrors": { "address": "주소를 입력해 주세요", "photos": "사진 또는 임베드가 1개 이상 필요해요", "photos[1].sourceUrl": "출처 링크를 입력해 주세요" } }
```

| 상태 | 언제 |
| --- | --- |
| 400 | 형식 검증 실패, 공개 시 검증 실패(`fieldErrors`), 주말 편성 |
| 401 | 비로그인 |
| 403 | 권한 없음, CSRF 토큰 없음 |
| (공통) | 필터 단계 오류(잘못된 URL 400, 세션 저장소 장애 500 등)도 `/error`가 상태를 유지한 채 `{ message }`로 준다 |
| 404 | 없음, 공개되지 않은 slug, 일반 사용자의 `/admin` 화면 |
| 409 | 전환표 밖 상태 전환, 공개 후 slug 변경, slug 중복, 중복 편성, 08:00 지난 오늘 칸 수정, 폐업 공간 저장, 같은 이름의 공개 공간 제보 |
| 429 | 제보 하루 10건 초과 |

### 커서 목록

모든 목록은 `?cursor=&size=`(size 기본 24, 최대 60)이고 응답은 같다. 커서는 정렬 키 `(정렬 컬럼, id)`를 base64로 감싼 불투명 문자열. 끝이면 `nextCursor: null`.

```json
{ "items": [ ... ], "nextCursor": "eyJ..." }
```

### 공통 모양

```jsonc
// PlaceTile — 격자·목록 한 칸. 둘러보기, 프로필 격자, 내 공간, 같은 만든 사람의 다른 공간
{ "id": 1, "slug": "quiet-hill-garden", "name": "고요언덕 정원", "region": "경기 양평군", "kind": "garden",
  "status": "published",                 // published | paused | closed
  "photo": Photo | null,                 // 대표 사진(position 0). 없으면 null → 프론트가 placeholder 색
  "caption": "조경" }                    // 화면마다 다름: 프로필 격자=역할, 팔로잉=만든 사람 이름, 둘러보기=없음(null)

// Photo
{ "url": { "w1600": "https://…/places/1/abc-1600.webp", "w800": "https://…/places/1/abc-800.webp" },
  "width": 1600, "height": 1067, "alt": "…", "credit": "○○",
  "license": "own" | "press" | "credited",     // own 직접 찍음 · press 언론·공식 배포용 · credited 공개된 사진을 출처 표기로 씀
  "sourceUrl": "https://…" | null }             // own이 아니면 필수. 화면은 "사진: {credit}"에 링크를 건다
// url은 서버가 PHOTO_PUBLIC_BASE_URL + objectKey로 만든 완성된 URL

// MakerSummary
{ "id": 3, "slug": "garden-dal", "name": "정원 달", "kind": "studio", "region": "경기 양평군" | null, "photo": Photo | null }
```

숫자(저장 수·팔로워 수·조회수)는 어떤 공개 응답에도 넣지 않는다.

---

## 1. 공개 조회 (로그인 불필요)

| 메서드 · 경로 | 응답 | 규칙 |
| --- | --- | --- |
| `GET /api/feed` | 아래 | 이번 주 월~금 중 `pick_date 08:00 KST <= now`, 최신순. 월 08:00 전은 지난주. 편성 없는 날은 건너뜀 |
| `GET /api/places?kind=garden,cafe&cursor=&size=` | `{ items: PlaceTile[], nextCursor }` | published·paused만, `published_at desc, id desc`. `kind`는 쉼표 다중 |
| `GET /api/places/{slug}` | 아래 | published·paused·closed만, 나머지 404 |
| `GET /api/makers?cursor=&size=` | `{ items: MakerSummary[], nextCursor }` | 공개(published·paused) 공간이 있는 사람만. 정렬: 가장 최근 공개된 공간의 `published_at desc, id desc` |
| `GET /api/makers/{slug}` | 아래 | 크레딧에 있는 공간 격자. 폐업 포함, draft·suggested 제외 |
| `GET /api/sitemap` | `{ places: [{slug, updatedAt}], makers: [{slug, updatedAt}] }` | 공개 공간·공개 공간 있는 만든 사람 전부. `app/sitemap.ts`가 쓴다 |

```jsonc
// GET /api/feed
{ "weekLabel": "2026-10-05",                    // 그 주 월요일 날짜
  "picks": [                                    // 최신순(오늘이 맨 위)
    { "date": "2026-10-07", "weekday": "WED", "line": "에디터의 한 줄(40자)",
      "place": PlaceTile,                        // photo = 대표 사진, caption = null
      "firstMaker": { "id": 3, "slug": "…", "name": "…", "role": "architecture" } | null } ],
  "openedCount": 3,                              // 이번 주 열린 카드 수(= picks 길이). 카드의 "03 / 05"는 이 값이 아니라 각 카드의 date(월 01~금 05)로 정한다
  "nextOpensAt": "2026-10-07T23:00:00Z",         // 다음 공개 시각(UTC). 금 08:00 뒤·주말은 다음 월 08:00. 다음 편성이 없어도 시각은 준다
  "nextWeekday": "THU" | "MON",                  // 마지막 화면 문구용. 프론트는 시간 계산을 하지 않는다
  "nextRelative": "today" | "tomorrow" | "later" } // 다음 공개일이 오늘(평일 08:00 전) · 내일(일요일 포함) · 그 밖. SPEC 6절 피드 마지막 화면 문구 셋에 1:1

// GET /api/places/{slug}
{ "id": 1, "slug": "…", "name": "…", "kind": "garden", "region": "경기 양평군",
  "address": "…", "lat": 37.5, "lng": 127.6, "access": "ticket", "visitNote": "…",  // lat·lng는 DB 값 그대로(폼에서는 받지 않음)
  "summary": "…", "impression": "…",
  "senses": { "light": "…", "sound": null, "scent": null, "touch": "…" },   // null은 줄째 숨김
  "essay": "마크다운" | null,
  "visitedByEditorOn": "2026-05" | null,         // 년-월만
  "status": "published",                        // paused → 띠, closed → 띠 + 저장 숨김
  "publishedAt": "…",
  "credits": [ { "position": 0, "role": "architecture", "verified": true, "sourceUrl": "…",
                 "maker": { "id": 3, "slug": "…", "name": "…", "kind": "person" } } ],
  "sources": [ { "title": "…", "url": "…", "type": "article" } ],
  "photos": [ Photo ],                           // position 순
  "embeds": [ { "type": "youtube", "url": "…" } ],
  "mapLinks": { "naver": "https://map.naver.com/…", "kakao": "https://map.kakao.com/…" },  // 서버가 "이름 지역" 검색으로 조립
  "relatedPlaces": [ PlaceTile ] }               // 같은 만든 사람의 다른 공간, 최대 6, caption = null

// GET /api/makers/{slug}
{ "id": 3, "slug": "…", "name": "…", "kind": "studio", "region": "경기 양평군" | null, "intro": "…",
  "website": "…" | null, "instagram": "…" | null,
  "affiliation": { "id": 4, "slug": "…", "name": "…" } | null,
  "places": [ PlaceTile ] }                      // 최근 공개순, caption = 그 사람의 역할, 폐업 포함(status로 흐리게)
```

**공개 조회 세부 규칙**

| 항목 | 규칙 |
| --- | --- |
| 공개 페이지가 있는 공간 | `published`·`paused`·`closed`이고 slug가 있는 공간. 상세·프로필 격자·sitemap이 이 기준 |
| 목록에 나오는 공간 | `published`·`paused`만. 둘러보기, `relatedPlaces`, 만든 사람 목록이 이 기준(폐업은 빠짐) |
| `relatedPlaces` | 이 공간의 크레딧에 있는 만든 사람 중 누구든 크레딧에 있는 다른 공간. 같은 공간은 한 번, `published_at desc, id desc`, 최대 6 |
| 만든 사람 프로필 404 | slug가 없거나, 공개 페이지가 있는 공간의 크레딧에 한 번도 없으면 404 |
| 프로필 격자 | 같은 공간에 크레딧이 둘 이상이면 한 칸, 캡션은 `position`이 가장 앞선 역할 |
| `caption`(역할) | 한국어 이름. planning 기획 · branding 브랜딩 · architecture 건축 · interior 인테리어 · landscape 조경 · lighting 조명 · furniture 가구 · scent 향 · photography 사진 · owner 주인 · other 그 밖 |
| 만든 사람 목록의 `photo` | 사진이 있는 공간 중 가장 최근에 공개된 공간의 대표 사진. 없으면 null |
| 사진 URL | `{PHOTO_PUBLIC_BASE_URL}/{objectKey}-1600.webp`, `-800.webp` |
| `mapLinks.kakao` | `https://map.kakao.com/link/search/{이름 지역}`(지역이 없으면 이름만). 좌표는 관리자 폼에서 받지 않아 쓰지 않는다 ([카카오맵 API 가이드 "검색 결과 바로가기"](https://apis.map.kakao.com/web/guide/)) |
| `mapLinks.naver` | `https://map.naver.com/p/search/{이름 지역}`. 네이버는 웹 URL 형식의 공식 문서가 없다(앱용 `nmap://`만 있음) |
| 목록 요청 오류 | 모르는 `kind`, 읽을 수 없는 `cursor`, 1~60 밖의 `size`는 400 |
| sitemap 만든 사람 | 프로필이 404가 아닌 사람 |
| slug가 null인 만든 사람 | `credits[].maker.slug`, `affiliation.slug`가 null이면 링크를 걸지 않는다(프로필이 없다) |

피드의 `place`가 `paused`여도 그대로 보여준다. 편성 뒤 `closed`가 된 공간은 카드에 "이제는 만날 수 없는 공간이에요"를 띄우고 저장 버튼을 숨긴다.

---

## 2. 로그인·세션

| 메서드 · 경로 | 요청 → 응답 | 규칙 |
| --- | --- | --- |
| `GET /oauth2/authorization/google?redirect=/places/x` | 302 → 구글 | Spring Security 기본 진입점. MVP 제공자는 구글만. `redirect`는 `/`로 시작하는 사이트 안 경로만 허용(`//`, 역슬래시, 제어 문자가 있으면 `/`), 세션에 저장. 구글에 주는 `redirect_uri`는 `FRONTEND_URL/login/oauth2/code/google` 고정 |
| `GET /login/oauth2/code/google` | 302 → `redirect`(상대 경로) | 콜백. `users` upsert(`(provider, providerId)` 기준). 새 사용자 닉네임은 구글 이름(없으면 이메일 앞부분, 그것도 없으면 "방문자")을 20자로 자른 값, 다시 로그인하면 닉네임은 그대로 두고 사진·이메일만 갱신. 이메일은 `email_verified=true`일 때만 저장하고 `ADMIN_EMAILS`와 비교 |
| (같은 콜백, 실패·취소) | 302 → `redirect?login=error` | 구글에서 취소(`error=access_denied`)하거나 검증이 실패하면. 원인은 서버 로그에만 |
| `GET /api/me` | `{ id, nickname, profileImageUrl, role, hasNewFromFollowing }` | 비로그인 401. 응답에 `XSRF-TOKEN` 쿠키가 실린다(프론트가 처음 부르는 요청) |
| `PATCH /api/me` | `{ nickname }` → 200 `/api/me`와 같음 | 1~20자 |
| `DELETE /api/me` | 204 | 세션을 지우고 `users` 행 삭제(저장·팔로우는 함께 지워지고 제보는 작성자만 비움, [SPEC.md 4절](SPEC.md#4-데이터)). 쿠키 만료 |
| `POST /api/auth/logout` | 204 | 세션 삭제, 쿠키 만료. 비로그인도 204(멱등) |
| `POST /api/dev/login` | `{ email }` → 200 `/api/me`와 같음 | **dev·test 프로필만.** 없으면 만들고(`provider=dev`, 닉네임은 이메일 앞부분) 세션 생성. 기존 세션이 있으면 세션 ID를 바꾼다 |

구글은 `email`을 항상 주지만 `users.email`은 nullable로 둔다(공개 후 카카오 등 제공자 추가 대비). `provider` 값은 `google` · `dev`.

---

## 3. 저장·팔로우·내 공간 (로그인 필요)

| 메서드 · 경로 | 요청 → 응답 | 규칙 |
| --- | --- | --- |
| `GET /api/me/state?places=1,2&makers=3` | `{ saves: { "1": "want" }, follows: [3] }` | 공개 화면 위 사용자별 값. 최대 100개씩(넘거나 숫자가 아니면 400). 저장·팔로우하지 않은 id는 빠진다 |
| `PUT /api/me/saves/{placeId}` | `{ status: "want" \| "been" }` → 200 `{ placeId, status }` | 같은 행 하나를 갱신(want↔been). 폐업 공간 409, 공개되지 않은·없는 공간 404 |
| `DELETE /api/me/saves/{placeId}` | 204 | 켜진 버튼을 다시 누름 = 삭제. 없어도 204 |
| `GET /api/me/saves?status=want&cursor=` | `{ items: PlaceTile[], nextCursor }` | `status` 필수. `updated_at desc`(상태를 바꾸면 그 목록의 맨 앞). caption = null. 저장한 뒤 폐업한 공간도 보인다(`status: closed`) |
| `PUT /api/me/follows/{makerId}` | 204 | 이미 있어도 204. 없는 만든 사람 404 |
| `DELETE /api/me/follows/{makerId}` | 204 | |
| `GET /api/me/following/places?cursor=` | `{ items: (PlaceTile & { isNew: boolean })[], nextCursor }` | 팔로우한 사람들의 공개 공간, 중복 제거, `published_at desc`. caption = 만든 사람 이름(여럿이면 첫 번째). `isNew = published_at > max(following_seen_at, 그 공간의 팔로우한 사람을 팔로우한 시각)`. 늦게 팔로우한 사람의 지난 공간은 새롭지 않다. 공간에 팔로우한 사람이 여럿이면 그중 한 명 기준으로라도 새로우면 true. 팔로잉 탭을 연 적이 없으면 팔로우 시각만 본다 |
| `GET /api/me/following/makers` | `{ items: MakerSummary[] }` | 최근 팔로우순. 커서 없음(수가 작다) |
| `POST /api/me/following/seen` | 204 | `following_seen_at = now`. 호출 전 응답의 `isNew`는 프론트가 화면을 떠날 때까지 유지 |

`hasNewFromFollowing`(`/api/me`)은 `GET /api/me/following/places`의 `isNew`가 하나라도 있는가와 같은 식이다.

---

## 4. 제보 (로그인 필요)

| 메서드 · 경로 | 요청 → 응답 | 규칙 |
| --- | --- | --- |
| `POST /api/suggestions/places` | `{ name, region, link?, reason, makerHint? }` → 201 `{ id, status: "suggested" }` | 같은 이름(앞뒤 공백·대소문자 무시)의 공개 공간(공개·휴업·폐업)이 있으면 409 `{ message, slug }`. 이름 100자 · 지역 50자 · 이유 300자 · 만든 사람 100자. `link`는 http·https만(빈 값은 없음) |
| `POST /api/suggestions/credits` | `{ placeId, role, makerName, sourceUrl?, isSelf? }` → 201 `{ id, status: "pending" }` | 공개 페이지가 없는 공간 404. `sourceUrl`은 http·https만. `isSelf`가 빠지면 false |
| `GET /api/me/suggestions` | `{ places: [{ id, name, region, status, createdAt }], credits: [{ id, placeName, role, makerName, status }] }` | 내 것만. 화면 문구: suggested·draft·pending=검토 중, published·paused·closed·accepted=소개됨, rejected·declined=보류 |

두 제보 합산 사용자당 하루(KST 자정 기준) 10건, 11번째 429 `{ message }`. 동시에 보내도 10건을 넘지 않는다(사용자 행을 잠근 뒤 센다).

---

## 5. 관리자 (`role=admin`)

### 공간

| 메서드 · 경로 | 요청 → 응답 | 규칙 |
| --- | --- | --- |
| `GET /api/admin/dashboard` | `{ picks: { thisWeek: Pick[5], nextWeek: Pick[5] }, pendingSuggestions, draftCount, llmCostThisMonthUsd }` | 관리자 홈. `Pick`은 아래 편성과 같고 빈 칸은 `place: null`·`line: null`. 이번 주 = 오늘(KST)이 든 달력 주의 월~금(주말에 보면 끝난 주), 다음 주 = 그다음 월~금. `pendingSuggestions` = 제보됨 공간 수 + 대기 중 크레딧 제보 수. `draftCount` = 작성 중 공간 수. `llmCostThisMonthUsd` = 이번 달(KST 1일 00:00부터) `llm_calls.cost_usd` 합, 없으면 0 |
| `GET /api/admin/places?status=&q=&cursor=` | `{ items: [{ id, slug, name, region, kind, status, updatedAt, hasPhoto, pickedOn }], nextCursor }` | `updated_at desc, id desc`. `pickedOn`은 편성된 날(`YYYY-MM-DD`) 또는 null — 공간은 한 번만 편성하므로 편성 화면이 이미 편성된 공간을 고르지 못하게 한다. `q`는 이름 부분 일치(대소문자 무시). `status`는 하나, 모르는 값 400 |
| `POST /api/admin/places` | `{ name, region? }` → 201 `AdminPlace` | `status=draft`. slug는 비워 두고 공개 전에 채운다 |
| `GET /api/admin/places/{id}` | `AdminPlace` | 수정 폼·미리보기. 공개 상세와 같은 하위 목록을 전부 포함 |
| `PATCH /api/admin/places/{id}` | `AdminPlace`의 스칼라 필드 전부 → 200 `AdminPlace` | 임시저장. 빠지거나 null·빈 문자열이면 비운다. `name`만 필수. 형식 검증만(길이·enum·slug 형식). 좌표(`lat`·`lng`)는 받지 않고 DB 값을 그대로 둔다. slug 중복 409. 공개 후 slug 변경 409(같은 값은 통과). 공개 페이지가 있는 상태(published·paused·closed)면 아래 공개 검증도 통과해야 한다(실패 400 `{ message: "공개된 공간이라 이 값은 비울 수 없어요", fieldErrors }`, 수정은 남지 않음) |
| `POST /api/admin/places/{id}/status` | `{ to }` → 200 `AdminPlace` | [SPEC.md 4절](SPEC.md#places) 전환표만, 밖이면(같은 상태로도) 409 `{ message: "공개 상태에서는 작성 중(으)로 바꿀 수 없어요" }`. `to=published`면 아래 공개 검증, 실패 400 `{ message: "공개할 수 없어요", fieldErrors }`. 처음 공개 때만 `published_at`(휴업 뒤 다시 공개해도 그대로). 모르는 `to` 400 |
| `PUT /api/admin/places/{id}/credits` | `[{ makerId, role, verified, sourceUrl? }]` → 200 `AdminPlace` | 통째 교체, 배열 순서 = position. `verified`인데 `sourceUrl` 없으면 400(`credits[i].sourceUrl`), 없는 만든 사람 400(`credits[i].makerId`) |
| `PUT /api/admin/places/{id}/sources` | `[{ title, url, type }]` → 200 `AdminPlace` | 통째 교체. `title` 200자 |
| `PUT /api/admin/places/{id}/embeds` | `[{ type, url }]` → 200 `AdminPlace` | 통째 교체. 공개 화면이 그릴 수 있는 주소만: youtube는 `youtube.com`·`www.youtube.com`·`m.youtube.com`의 `watch?v={id}`·`/shorts/{id}`·`/embed/{id}` 또는 `youtu.be/{id}`, instagram은 `instagram.com`·`www.instagram.com`의 `/p/{code}`·`/reel/{code}`. 프로필·채널·홈 주소는 400 `embeds[i].url`("유튜브 영상 주소를 넣어 주세요" / "인스타그램 게시물 주소를 넣어 주세요") |

**목록 교체 공통**: 링크(`sourceUrl`, `url`)는 https만("https://로 시작하는 주소를 넣어 주세요"), 500자. 앞뒤 공백은 지우고 빈 칸은 null. 형식 오류의 키는 `credits[0].role`처럼 목록 이름 + 원소 번호 + 필드(원소가 null이면 `embeds[0]`: "빈 항목은 보낼 수 없어요"). 모르는 enum 값은 칸 이름 없는 400. 공개 페이지가 있는 상태면 교체 뒤 공개 검증도 통과해야 한다(실패 400 `{ message: "공개된 공간이라 이 값은 비울 수 없어요", fieldErrors }`, 교체는 남지 않음). 성공하면 공간의 `updatedAt`이 바뀐다.

```jsonc
// AdminPlace = 공개 상세(1절)의 모든 필드 + 아래
{ …, "status": "draft", "suggestedBy": { "id": 9, "nickname": "…" } | null, "suggestionReason": "…" | null,
  "suggestionLink": "…" | null, "suggestionMakerHint": "…" | null,
  "createdAt": "…", "updatedAt": "…", "slugLocked": true,          // published_at이 있으면 true
  "photos": [ Photo & { "id": 11, "objectKey": "uploads/1/<uuid>", "position": 0 } ],
  "credits": [ … & { "makerId": 3 } ] }
// senses·impression 등 빈 값은 null. 미리보기는 이 응답을 공개 상세 컴포넌트에 그대로 넣는다.
// PATCH 요청은 이 응답의 스칼라 필드 모양 그대로(lat·lng 빼고): slug, name, kind, region, address, access, visitNote,
// summary, impression, senses { light, sound, scent, touch }, essay, visitedByEditorOn("2026-05", 1일로 저장).
```

### 공개 시 검증 — `PublishRules`

`published`로 바꿀 때와 공개 페이지가 있는 공간을 고칠 때 서버가 검사한다. 관리자 폼의 공개 검증 스키마(Zod)는 이 표를 1:1로 옮긴다(키·조건·문구). 여러 개가 함께 실패하면 모두 이 순서로 준다. 길이·형식은 PATCH의 형식 검증이 먼저 막는다.

| 키 | 실패 조건 | 문구 |
| --- | --- | --- |
| `slug` | 없음 | 주소(slug)를 정해 주세요 |
| `name` | 비어 있음 | 공간 이름을 입력해 주세요 |
| `kind` | 없음 | 종류를 골라 주세요 |
| `region` | 없음 | 지역을 입력해 주세요 |
| `address` | 없음 | 주소를 입력해 주세요 |
| `access` | 없음 | 방문 방식을 골라 주세요 |
| `summary` | 없음 | 한 줄 소개를 입력해 주세요 |
| `impression` | 없음 | 감상을 입력해 주세요 |
| `credits` | `sourceUrl`이 있는 크레딧이 0개(확인 여부는 보지 않는다) | 출처가 있는 크레딧이 1개 이상 필요해요 |
| `sources` | `visitedByEditorOn`이 없고 출처가 0개 | 머물러 본 날이 없으면 출처가 1개 이상 필요해요 |
| `photos` | 사진 0장이고 임베드 0개 | 사진 또는 임베드가 1개 이상 필요해요 |
| `photos[i].alt` | 비어 있음 | 사진 설명을 입력해 주세요 |
| `photos[i].credit` | 비어 있음 | 사진 출처(이름)를 입력해 주세요 |
| `photos[i].sourceUrl` | `license`가 `own`이 아닌데 없음 | 출처 링크를 입력해 주세요 |

`i`는 `photos` 배열(position 순)의 인덱스. "없음·비어 있음"은 null 또는 공백뿐인 문자열. `visitNote`·`senses`·`essay`·`visitedByEditorOn`은 선택이다.

### 사진

| 메서드 · 경로 | 요청 → 응답 | 규칙 |
| --- | --- | --- |
| `POST /api/admin/uploads` | `{ placeId, contentType: "image/webp", sizes: { w1600, w800 } }` → `{ objectKey, uploadUrls: { w1600, w800 } }` | PUT 서명 URL 5분. `contentType`과 `sizes`(바이트)가 서명에 들어가 브라우저는 `Content-Type: image/webp`와 밝힌 크기로만 올릴 수 있다(다르면 R2 403 SignatureDoesNotMatch — 개발 버킷에서 확인). `image/webp` 외 400(`contentType`: "WebP 사진만 올릴 수 있어요"), 크기가 없거나 0이면 400("사진 크기가 필요해요"), 한 파일이 5MB를 넘으면 400(`sizes.w1600`·`sizes.w800`: "사진 파일이 너무 커요(5MB까지)"), 없는 공간 404. 원본 파일 한도 20MB는 브라우저가 고를 때 본다(서버는 원본을 받지 않고, 바꾼 WebP만 저장소에 간다). objectKey = `uploads/{placeId}/{uuid}`, 파일은 `{objectKey}-1600.webp`·`-800.webp`. R2 서명 URL은 `*.r2.cloudflarestorage.com` 엔드포인트로만 발급된다(커스텀 도메인 불가) |
| `POST /api/admin/places/{id}/photos` | `{ objectKey, width, height, alt, credit, license, sourceUrl? }` → 201 `AdminPlace` | position은 맨 뒤. `objectKey`가 이 공간에 발급한 모양이 아니면 400 `objectKey`("이 공간의 업로드 키가 아니에요"), 두 크기 파일이 저장소에 없으면 400 `objectKey`("업로드가 끝나지 않은 사진이에요"). `license`가 `own`이 아닌데 `sourceUrl` 없으면 400 `sourceUrl`. 같은 키 두 번 409 "이미 등록된 사진이에요" |
| `PATCH /api/admin/places/{id}/photos/{photoId}` | `{ alt, credit, license, sourceUrl }` → 200 `AdminPlace` | 공간 PATCH처럼 네 칸 전부(빠지거나 빈 칸이면 비움, `license`만 필수). 같은 출처 규칙. 없는 사진 404 |
| `PUT /api/admin/places/{id}/photos/order` | `{ photoIds: [11, 12] }` → 200 `AdminPlace` | 배열 순서 = position, 첫 번째가 대표. 지금 사진 id 전부와 정확히 같아야 한다(빠짐·중복·남의 id 400 `photoIds`: "사진 목록이 맞지 않아요") |
| `DELETE /api/admin/places/{id}/photos/{photoId}` | 204 | 남은 사진의 position을 0부터 다시 매긴다. 두 크기 파일은 커밋 뒤에 지우고, 실패하면 로그만 남긴다(다음 고아 정리가 지운다) |

사진 등록·수정·순서·삭제도 목록 교체처럼 공개 페이지가 있는 상태면 공개 검증을 통과해야 한다(공개된 공간의 마지막 사진을 임베드 없이 지우면 400, 아무것도 바뀌지 않는다). 형식: `alt` 200자, `credit` 100자, `sourceUrl` https 500자, `width`·`height` 1 이상.

고아 정리: `@Scheduled` 매일 03:00 KST, `uploads/` 접두사의 파일 중 업로드 키(`-1600.webp`·`-800.webp`를 뗀 것)가 `place_photos.object_key`에 없고 24시간 이상 지난 것을 지운다. 실패는 로그만.

### 만든 사람

| 메서드 · 경로 | 요청 → 응답 | 규칙 |
| --- | --- | --- |
| `GET /api/admin/makers?q=&cursor=&size=` | `{ items: [{ id, slug, name, kind, region, placeCount, updatedAt }], nextCursor }` | `q` 이름 부분 일치(대소문자 무시, 크레딧 콤보박스가 쓴다). `updated_at desc, id desc`. `placeCount`는 크레딧에 있는 공간 수(상태 무관) |
| `POST /api/admin/makers` | `{ name, kind, slug?, region?, intro?, website?, instagram?, affiliationId? }` → 201 `AdminMaker` | slug 중복 409 |
| `GET /api/admin/makers/{id}` | `AdminMaker` | |
| `PATCH /api/admin/makers/{id}` | POST와 같은 모양, 필드 전부 → 200 `AdminMaker` | 빠지거나 null·빈 칸이면 비운다(공간 PATCH와 같은 이유). slug 잠금 409, slug 중복 409 |

**만든 사람 검증**: `name` 필수 100자, `kind` 필수, `slug` 80자·영문 소문자·숫자·하이픈, `region` 50자, `intro` 200자, `website` https 500자, `instagram`은 `@` 없는 아이디(영문·숫자·`.`·`_` 30자, 앞의 `@`는 지운다), `affiliationId`는 있는 다른 만든 사람(없으면·자기 자신이면 400 `affiliationId`). **slug 잠금**: `makers.slug_locked_at`이 있으면 slug를 바꾸거나 비울 수 없다(409). 잠그는 때는 셋이다 — 공간이 처음 공개될 때 그 공간 크레딧의 만든 사람, 공개 페이지가 있는 공간(published·paused·closed)의 크레딧을 교체할 때 그 크레딧의 만든 사람, 이미 공개된 공간의 크레딧에 있는 사람에게 slug를 처음 정할 때. slug가 없으면 잠그지 않는다(처음 정하는 것은 된다). 한 번 잠기면 크레딧에서 빠져도 풀리지 않는다. 시각은 서버 `Clock`.

`AdminMaker` = 1절 `GET /api/makers/{slug}` 응답 + `slugLocked`, `createdAt`, `updatedAt`. `places`는 공개 프로필과 같은 목록(공개 페이지가 있는 공간)이라 작성 중인 공간은 빠진다. 프로필이 404인 사람도 응답한다.

### 편성

| 메서드 · 경로 | 요청 → 응답 | 규칙 |
| --- | --- | --- |
| `GET /api/admin/picks?from=2026-10-05&to=2026-10-16` | `{ items: Pick[] }` | 범위 안의 평일 전부, 날짜순. 편성이 없는 날은 빈 칸(`place: null`). 두 날짜 필수, `from > to`·62일 이상 400. 편성 뒤 휴업·폐업한 공간도 칸에 그대로 보인다 |
| `PUT /api/admin/picks/{date}` | `{ placeId, line }` → 200 `Pick` | 월~금만(주말 400 "월~금만 편성할 수 있어요"), 열린 칸(`locked`) 409 "이미 열린 날은 바꿀 수 없어요", 없는 공간 404, published만(아니면 409 "공개된 공간만 편성할 수 있어요"), 다른 날에 편성된 공간 409 "이미 다른 날에 편성된 공간이에요". 같은 날 칸이 있으면 공간·한 줄을 바꾼다. `line` 앞뒤 공백을 지운 뒤 1~40자(400 `line`) |
| `DELETE /api/admin/picks/{date}` | 204 | 같은 주말·잠금 규칙. 편성이 없으면 404 |

```jsonc
// Pick
{ "date": "2026-10-07", "weekday": "WED", "line": "…" | null, "locked": false,   // locked = pick_date 08:00 KST <= 지금(피드에 열린 칸). 피드 공개와 같은 경계
  "place": { "id": 1, "slug": "…", "name": "…", "photo": Photo | null } | null }
```

### 제보 검토

| 메서드 · 경로 | 요청 → 응답 | 규칙 |
| --- | --- | --- |
| `GET /api/admin/suggestions?type=places\|credits&status=` | `{ items: [...] }` | places: `{ id, name, region, link, reason, makerHint, suggestedBy: { id, nickname } \| null, createdAt }` · credits: `{ id, place: {id, slug, name}, role, makerName, sourceUrl, isSelf, user: { id, nickname } \| null, status, createdAt }`. 먼저 온 것부터(`created_at`, `id`). `status` 기본은 places `suggested`, credits `pending`. places는 `suggested`·`rejected`, credits는 `pending`·`accepted`·`declined`만, 그 밖·`type` 없음 400. 탈퇴한 사람은 null. 페이지 없음 |
| `POST /api/admin/suggestions/places/{placeId}/start` | → 200 `AdminPlace` | suggested → draft. 폼으로 이동. 상태 전환과 같은 길이라 제보됨이 아니면 409 |
| `POST /api/admin/suggestions/places/{placeId}/reject` | → 204 | suggested → rejected. 같은 409 |
| `POST /api/admin/suggestions/credits/{id}/accept` | `{ makerId, verified, sourceUrl? }` → 200 `AdminPlace` | 역할은 제보의 역할, `sourceUrl`이 없으면 제보의 출처. 크레딧 목록 교체와 같은 길로 맨 뒤에 추가(없는 만든 사람 400 `makerId`, 확인됨인데 출처 없음 400 `sourceUrl`, slug 잠금·공개 검증 그대로). `status=accepted`, `reviewed_at`. 대기 중이 아니면 409 "이미 검토한 제보예요" |
| `POST /api/admin/suggestions/credits/{id}/decline` | → 204 | `status=declined`, `reviewed_at`. 같은 409 |

---

## 6. 내부 계약 (서비스 사이)

### Spring → Next `POST {REVALIDATE_URL}/api/revalidate`

```jsonc
// 헤더 X-Revalidate-Secret: ${REVALIDATE_SECRET} (없거나 다르면 401)
{ "tags": ["place:quiet-hill-garden", "maker:garden-dal", "browse"] }
// 응답 200 { "revalidated": ["…"] }
// 라우트는 태그마다 revalidateTag(tag, { expire: 0 }) — 다음 요청이 옛 페이지 없이 새로 만든다
```

| 태그 | 붙는 페이지 | 보내는 때 |
| --- | --- | --- |
| `feed` | `/` | 월~금 08:00:05 KST 스케줄러(공개), 매일 00:00:05 KST 스케줄러(마지막 화면의 "내일"이 "오늘"로 바뀜), 편성 등록·삭제, 공간·만든 사람 변경(피드 카드가 이름·대표 사진·첫 만든 사람을 보여준다) |
| `browse` | `/places`, `/makers` | 공간 공개·수정·상태 변경, 만든 사람 수정 |
| `place:{slug}` | `/places/[slug]`, 그 OG 이미지 | 공간 공개·수정·상태 변경, 크레딧에 있는 만든 사람 수정 |
| `maker:{slug}` | `/makers/[slug]`, 그 OG 이미지 | 만든 사람 수정(잠기기 전 slug를 바꿨으면 옛 slug도), 그 사람이 크레딧에 있거나 크레딧에서 빠진 공간의 공개·수정·상태 변경 |
| `sitemap` | `/sitemap.xml` | 공간 처음 공개, 만든 사람 처음 공개 공간 연결 |

공간 변경은 바뀐 뒤 상태가 공개 페이지가 있는 상태(published·paused·closed)일 때만 보낸다. 수정·상태 전환·크레딧·출처·임베드 교체·사진 추가·수정·순서·삭제·크레딧 제보 수락이 모두 같은 규칙이다. 만든 사람 수정은 그 사람이 크레딧에 있는 공개 공간이 있을 때만 보낸다.

호출은 트랜잭션 커밋 뒤에만(`AFTER_COMMIT`). 실패는 로그만, 재시도 없음. 재생성 호출이 빠져도 서버가 공개 데이터를 1시간마다 다시 받는다(`serverFetch`의 `revalidate: 3600`).

### 헬스

`backend`: `GET /actuator/health` · `frontend`: `GET /` 200. compose healthcheck가 이것들을 본다.

---

## 7. 환경 변수

이름과 설명은 [.env.example](../.env.example)(개발)과 [deploy/.env.example](../deploy/.env.example)(운영), 운영 값을 넣는 순서는 [RUNBOOK.md](RUNBOOK.md)에 있습니다.
