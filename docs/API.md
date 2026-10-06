# API 목록 (API v0.2 — 구현 전 초안)

> 2026-10-06. 공개·사용자·관리자 API와 서비스 사이 내부 계약의 경로·요청·응답을 한 곳에 모았다. **SPEC에는 API가 없으므로 이 문서가 API의 기준**이다. 구현하다 모양을 바꿔야 하면 코드보다 이 문서를 먼저 고친다.
> 필드 이름은 SPEC 2절 코드 이름을 따르고 JSON은 camelCase. 시각은 ISO-8601 UTC(`2026-10-07T23:00:00Z`), 날짜는 `YYYY-MM-DD`.
> 아직 가정인 것은 **(가정)**으로 표시했다. 반대가 없으면 그대로 구현한다.

## 0. 공통

### 인증·보호

| 구분 | 규칙 |
| --- | --- |
| 세션 | Spring Session JDBC. 쿠키 `SESSION`, HttpOnly · SameSite=Lax · 운영 Secure |
| CSRF | 켬(Spring Security 7 `csrf.spa()`). 서버가 `XSRF-TOKEN` 쿠키(HttpOnly 아님)를 주고, 프론트는 GET 외 요청에 `X-XSRF-TOKEN` 헤더로 돌려보낸다. 없으면 403. 로그인·로그아웃 성공 시 쿠키가 지워지므로 그 뒤 `GET /api/me`로 다시 받는다 |
| CORS | `FRONTEND_URL` 한 출처만, credentials 허용 |
| 공개 | `/api/feed`, `/api/places/**`, `/api/makers/**`, `/api/sitemap` |
| 로그인 필요 | `/api/me/**`, `/api/suggestions/**`, `/api/auth/logout` → 비로그인 401 |
| 관리자 | `/api/admin/**` → `role=admin` 아니면 403 |
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
| 404 | 없음, 공개되지 않은 slug, 일반 사용자의 `/admin` 화면 |
| 409 | 전환표 밖 상태 전환, 공개 후 slug 변경, slug 중복, 중복 편성, 08:00 지난 오늘 칸 수정, 폐업 공간 저장, 같은 이름의 공개 공간 제보 |
| 429 | 제보 하루 10건 초과 |
| 504 | AI 초안 시간 초과(90초) |

### 커서 목록

모든 목록은 `?cursor=&size=`(size 기본 24, 최대 60)이고 응답은 같다. 커서는 정렬 키 `(정렬 컬럼, id)`를 base64로 감싼 불투명 문자열. 끝이면 `nextCursor: null`.

```json
{ "items": [ ... ], "nextCursor": "eyJ..." }
```

### 공통 모양

```jsonc
// PlaceTile — 격자·목록 한 칸. 둘러보기, 프로필 격자, 내 공간, 같은 만든 사람의 다른 공간
{ "id": 1, "slug": "medongol-garden", "name": "메덩골정원", "region": "경기 양평군", "kind": "garden",
  "status": "published",                 // published | paused | closed
  "photo": Photo | null,                 // 대표 사진(position 0). 없으면 null → 프론트가 placeholder 색
  "caption": "조경" }                    // 화면마다 다름: 프로필 격자=역할, 팔로잉=만든 사람 이름, 둘러보기=없음(null)

// Photo
{ "url": { "w1600": "https://…/places/1/abc-1600.webp", "w800": "https://…/places/1/abc-800.webp" },
  "width": 1600, "height": 1067, "alt": "…", "credit": "○○",
  "license": "own" | "press" | "credited",     // own 직접 찍음 · press 언론·공식 배포용 · credited 공개된 사진을 출처 표기로 씀
  "sourceUrl": "https://…" | null }             // own이 아니면 필수. 화면은 "사진: {credit}"에 링크를 건다
// url은 서버가 PHOTO_PUBLIC_BASE_URL + objectKey로 만든 완성된 URL (PLAN 확정 10)

// MakerSummary
{ "id": 3, "slug": "iroje", "name": "이로재", "kind": "studio", "region": "서울 종로" | null, "photo": Photo | null }
```

숫자(저장 수·팔로워 수·조회수)는 어떤 공개 응답에도 넣지 않는다.

---

## 1. 공개 조회 (로그인 불필요)

| 메서드 · 경로 | 응답 | 규칙 |
| --- | --- | --- |
| `GET /api/feed` | 아래 | 이번 주 월~금 중 `pick_date 08:00 KST <= now`, 최신순. 월 08:00 전은 지난주. 편성 없는 날은 건너뜀 |
| `GET /api/places?kind=garden,cafe&cursor=&size=` | `{ items: PlaceTile[], nextCursor }` | published·paused만, `published_at desc, id desc`. `kind`는 쉼표 다중 |
| `GET /api/places/{slug}` | 아래 | published·paused·closed만, 나머지 404 |
| `GET /api/makers?cursor=&size=` | `{ items: MakerSummary[], nextCursor }` | 공개(published·paused) 공간이 있는 사람만. 정렬: 가장 최근 공개된 공간의 `published_at desc` (가정) |
| `GET /api/makers/{slug}` | 아래 | 크레딧에 있는 공간 격자. 폐업 포함, draft·suggested 제외 |
| `GET /api/sitemap` | `{ places: [{slug, updatedAt}], makers: [{slug, updatedAt}] }` | 공개 공간·공개 공간 있는 만든 사람 전부. `app/sitemap.ts`가 쓴다 |

```jsonc
// GET /api/feed
{ "weekLabel": "2026-10-05",                    // 그 주 월요일 날짜
  "picks": [                                    // 최신순(오늘이 맨 위)
    { "date": "2026-10-07", "weekday": "WED", "line": "에디터의 한 줄(40자)",
      "place": PlaceTile,                        // photo = 대표 사진, caption = null
      "firstMaker": { "id": 3, "slug": "…", "name": "…", "role": "architecture" } | null } ],
  "openedCount": 3,                              // 진행 표시 "03 / 05"의 왼쪽. 공개 서비스 원칙의 "숫자"가 아니라 피드 위치다
  "nextOpensAt": "2026-10-07T23:00:00Z" | null,  // 다음 공개 시각(UTC). 금 08:00 뒤·주말은 다음 월 08:00. 다음 편성이 없어도 시각은 준다
  "nextWeekday": "THU" | "MON" }                 // 마지막 화면 문구용. 프론트는 시간 계산을 하지 않는다

// GET /api/places/{slug}
{ "id": 1, "slug": "…", "name": "…", "kind": "garden", "region": "경기 양평군",
  "address": "…", "lat": 37.5, "lng": 127.6, "access": "ticket", "visitNote": "…",
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
  "mapLinks": { "naver": "https://map.naver.com/…", "kakao": "https://map.kakao.com/…" },  // 서버가 이름·좌표로 조립 (가정)
  "relatedPlaces": [ PlaceTile ] }               // 같은 만든 사람의 다른 공간, 최대 6, caption = null

// GET /api/makers/{slug}
{ "id": 3, "slug": "…", "name": "…", "kind": "studio", "region": "서울 종로" | null, "intro": "…",
  "website": "…" | null, "instagram": "…" | null,
  "affiliation": { "id": 4, "slug": "…", "name": "…" } | null,
  "places": [ PlaceTile ] }                      // 최근 공개순, caption = 그 사람의 역할, 폐업 포함(status로 흐리게)
```

피드의 `place`가 `paused`여도 그대로 보여준다. `closed`가 된 공간이 편성에 남아 있으면 카드에 "이제는 만날 수 없는 공간이에요" 띠(가정, 편성 뒤 폐업은 드물다).

---

## 2. 로그인·세션

| 메서드 · 경로 | 요청 → 응답 | 규칙 |
| --- | --- | --- |
| `GET /oauth2/authorization/google?redirect=/places/x` | 302 → 구글 | Spring Security 기본 진입점. MVP 제공자는 구글만. `redirect`는 `/`로 시작하는 경로만 허용, 세션에 저장 |
| `GET /login/oauth2/code/google` | 302 → `FRONTEND_URL + redirect` | 콜백. `users` upsert(`(provider, providerId)` 기준). 이메일이 `ADMIN_EMAILS`에 있으면 `role=admin` |
| `GET /api/me` | `{ id, nickname, profileImageUrl, role, hasNewFromFollowing }` | 비로그인 401. 응답에 `XSRF-TOKEN` 쿠키가 실린다(프론트가 처음 부르는 요청) |
| `PATCH /api/me` | `{ nickname }` → 200 `/api/me`와 같음 | 1~20자 |
| `DELETE /api/me` | 204 | SPEC 3절 탈퇴 순서. 세션 무효화 |
| `POST /api/auth/logout` | 204 | 세션 삭제, 쿠키 만료 |
| `POST /api/dev/login` | `{ email }` → 200 `/api/me`와 같음 | **dev·test 프로필만.** 없으면 만들고(`provider=dev`) 세션 생성 |

구글은 `email`을 항상 주지만 `users.email`은 nullable로 둔다(공개 후 카카오 등 제공자 추가 대비). `provider` 값은 `google` · `dev`.

---

## 3. 저장·팔로우·내 공간 (로그인 필요)

| 메서드 · 경로 | 요청 → 응답 | 규칙 |
| --- | --- | --- |
| `GET /api/me/state?places=1,2&makers=3` | `{ saves: { "1": "want" }, follows: [3] }` | 공개 화면 위 사용자별 값. 최대 60개씩 |
| `PUT /api/me/saves/{placeId}` | `{ status: "want" \| "been" }` → 200 `{ placeId, status }` | 같은 행 하나를 갱신(want↔been). 폐업 공간 409 |
| `DELETE /api/me/saves/{placeId}` | 204 | 켜진 버튼을 다시 누름 = 삭제. 없어도 204 |
| `GET /api/me/saves?status=want&cursor=` | `{ items: PlaceTile[], nextCursor }` | `updated_at desc`. caption = null |
| `PUT /api/me/follows/{makerId}` | 204 | 이미 있어도 204 |
| `DELETE /api/me/follows/{makerId}` | 204 | |
| `GET /api/me/following/places?cursor=` | `{ items: (PlaceTile & { isNew: boolean })[], nextCursor }` | 팔로우한 사람들의 공개 공간, 중복 제거, `published_at desc`. caption = 만든 사람 이름(여럿이면 첫 번째). `isNew = published_at > following_seen_at` |
| `GET /api/me/following/makers` | `{ items: MakerSummary[] }` | 팔로우순. 커서 없음(수가 작다) |
| `POST /api/me/following/seen` | 204 | `following_seen_at = now`. 호출 전 응답의 `isNew`는 프론트가 화면을 떠날 때까지 유지 |

`hasNewFromFollowing`(`/api/me`)은 `GET /api/me/following/places`의 `isNew`가 하나라도 있는가와 같은 식이다.

---

## 4. 제보 (로그인 필요)

| 메서드 · 경로 | 요청 → 응답 | 규칙 |
| --- | --- | --- |
| `POST /api/suggestions/places` | `{ name, region, link?, reason, makerHint? }` → 201 `{ id, status: "suggested" }` | 같은 이름의 공개 공간이 있으면 409 `{ message, slug }` |
| `POST /api/suggestions/credits` | `{ placeId, role, makerName, sourceUrl?, isSelf }` → 201 `{ id, status: "pending" }` | |
| `GET /api/me/suggestions` | `{ places: [{ id, name, region, status, createdAt }], credits: [{ id, placeName, role, makerName, status }] }` | 내 것만. 화면 문구: suggested·pending=검토 중, published=소개됨, rejected·declined=보류 |

두 제보 합산 사용자당 하루(KST) 10건, 11번째 429.

---

## 5. 관리자 (`role=admin`)

### 공간

| 메서드 · 경로 | 요청 → 응답 | 규칙 |
| --- | --- | --- |
| `GET /api/admin/dashboard` | `{ picks: { thisWeek: Pick[5], nextWeek: Pick[5] }, pendingSuggestions, draftCount, llmCostThisMonthUsd }` | 관리자 홈. `Pick`은 6절과 같고 빈 칸은 `place: null` |
| `GET /api/admin/places?status=&q=&cursor=` | `{ items: [{ id, slug, name, region, kind, status, updatedAt, hasPhoto }], nextCursor }` | `updated_at desc`. `q`는 이름 부분 일치 |
| `POST /api/admin/places` | `{ name, region? }` → 201 `AdminPlace` | `status=draft`. slug는 비워 두고 공개 전에 채운다 |
| `GET /api/admin/places/{id}` | `AdminPlace` | 수정 폼·미리보기. 공개 상세와 같은 하위 목록을 전부 포함 |
| `PATCH /api/admin/places/{id}` | `AdminPlace`의 스칼라 필드 부분 → 200 `AdminPlace` | 형식 검증만(길이·enum·slug 형식). slug 중복 409. 공개 후 slug 변경 409 |
| `POST /api/admin/places/{id}/status` | `{ to }` → 200 `AdminPlace` | SPEC 3절 전환표만, 밖이면 409. `to=published`면 `PublishRules` 검사, 실패 400 `fieldErrors`. 처음 공개 때 `published_at` |
| `PUT /api/admin/places/{id}/credits` | `[{ makerId, role, verified, sourceUrl? }]` → 200 `AdminPlace` | 통째 교체, 배열 순서 = position. `verified`인데 `sourceUrl` 없으면 400 |
| `PUT /api/admin/places/{id}/sources` | `[{ title, url, type }]` → 200 `AdminPlace` | 통째 교체 |
| `PUT /api/admin/places/{id}/embeds` | `[{ type, url }]` → 200 `AdminPlace` | 통째 교체 |

```jsonc
// AdminPlace = 공개 상세(1절)의 모든 필드 + 아래
{ …, "status": "draft", "suggestedBy": { "id": 9, "nickname": "…" } | null, "suggestionReason": "…" | null,
  "createdAt": "…", "updatedAt": "…", "slugLocked": true,          // published_at이 있으면 true
  "photos": [ Photo & { "id": 11, "objectKey": "places/1/abc", "position": 0 } ],
  "credits": [ … & { "makerId": 3 } ] }
// senses·impression 등 빈 값은 null. 미리보기는 이 응답을 공개 상세 컴포넌트에 그대로 넣는다.
```

### 사진

| 메서드 · 경로 | 요청 → 응답 | 규칙 |
| --- | --- | --- |
| `POST /api/admin/uploads` | `{ placeId, contentType: "image/webp" }` → `{ objectKey, uploadUrls: { w1600, w800 } }` | PUT 서명 URL 5분. `image/webp` 외 400. objectKey = `uploads/{placeId}/{uuid}`(접두사만 구분, 등록 여부는 DB로 판단 — 가정). R2 서명 URL은 `*.r2.cloudflarestorage.com` 엔드포인트로만 발급된다(커스텀 도메인 불가) |
| `POST /api/admin/places/{id}/photos` | `{ objectKey, width, height, alt, credit, license, sourceUrl? }` → 201 `AdminPlace` | position은 맨 뒤. `license`가 `own`이 아닌데 `sourceUrl` 없으면 400 |
| `PATCH /api/admin/places/{id}/photos/{photoId}` | `{ alt?, credit?, license?, sourceUrl? }` → 200 `AdminPlace` | 같은 규칙 |
| `PUT /api/admin/places/{id}/photos/order` | `{ photoIds: [11, 12] }` → 200 `AdminPlace` | 배열 순서 = position, 첫 번째가 대표 |
| `DELETE /api/admin/places/{id}/photos/{photoId}` | 204 | 객체도 삭제 |

고아 정리: `@Scheduled` 매일 03:00 KST, `uploads/` 접두사 중 `place_photos.object_key`에 없고 24시간 지난 객체 삭제.

### 만든 사람

| 메서드 · 경로 | 요청 → 응답 | 규칙 |
| --- | --- | --- |
| `GET /api/admin/makers?q=&cursor=` | `{ items: [{ id, slug, name, kind, region, placeCount }], nextCursor }` | `q` 이름 부분 일치(크레딧 콤보박스가 쓴다) |
| `POST /api/admin/makers` | `{ name, kind, slug?, region?, intro?, website?, instagram?, affiliationId? }` → 201 `AdminMaker` | |
| `GET /api/admin/makers/{id}` | `AdminMaker` | |
| `PATCH /api/admin/makers/{id}` | 부분 → 200 `AdminMaker` | 공개 공간에 연결된 뒤 slug 변경 409. slug 중복 409 |

`AdminMaker` = 1절 `GET /api/makers/{slug}` 응답 + `slugLocked`, `createdAt`, `updatedAt`.

### 편성

| 메서드 · 경로 | 요청 → 응답 | 규칙 |
| --- | --- | --- |
| `GET /api/admin/picks?from=2026-10-05&to=2026-10-16` | `{ items: Pick[] }` | 날짜 범위 안의 편성 |
| `PUT /api/admin/picks/{date}` | `{ placeId, line }` → 200 `Pick` | 월~금만(주말 400), published만(아니면 409), 이미 편성된 공간 409, 지난 날짜·오늘 08:00 지난 오늘 칸 409. `line` 1~40자 |
| `DELETE /api/admin/picks/{date}` | 204 | 같은 잠금 규칙 |

```jsonc
// Pick
{ "date": "2026-10-07", "weekday": "WED", "line": "…", "locked": false,   // locked = 수정 불가(지난 날·오늘 08:00 뒤)
  "place": { "id": 1, "slug": "…", "name": "…", "photo": Photo | null } | null }
```

### 제보 검토

| 메서드 · 경로 | 요청 → 응답 | 규칙 |
| --- | --- | --- |
| `GET /api/admin/suggestions?type=places\|credits&status=` | `{ items: [...] }` | places: `{ id, name, region, link, reason, makerHint, suggestedBy, createdAt }` · credits: `{ id, place: {id, slug, name}, role, makerName, sourceUrl, isSelf, user, status, createdAt }` |
| `POST /api/admin/suggestions/places/{placeId}/start` | → 200 `AdminPlace` | suggested → draft. 폼으로 이동 |
| `POST /api/admin/suggestions/places/{placeId}/reject` | → 204 | suggested → rejected |
| `POST /api/admin/suggestions/credits/{id}/accept` | `{ makerId, verified, sourceUrl? }` → 200 `AdminPlace` | 크레딧 맨 뒤에 추가, `status=accepted`, `reviewed_at` |
| `POST /api/admin/suggestions/credits/{id}/decline` | → 204 | `status=declined` |

### AI 초안·호출 기록

| 메서드 · 경로 | 요청 → 응답 | 규칙 |
| --- | --- | --- |
| `POST /api/admin/drafts` | `{ name, region? }` → 200 AI 응답 그대로(6절) | 내부망으로 AI `POST /drafts`, 타임아웃 90초 → 504. 성공·실패 모두 `llm_calls` 한 행 |
| `GET /api/admin/llm-calls/summary?from=&to=` | `{ calls, inputTokens, outputTokens, costUsd, avgLatencyMs, failureRate }` | 기간은 `created_at` 기준 |
| `GET /api/admin/llm-calls?cursor=` | `{ items: [{ id, purpose, placeName, model, inputTokens, outputTokens, costUsd, latencyMs, retries, success, createdAt }], nextCursor }` | 최신순 |

---

## 6. 내부 계약 (서비스 사이)

### Spring → AI 초안 서비스 `POST {AI_URL}/drafts`

A6에서 이 모양을 고정하고, B14가 그대로 구현한다. 바꿀 일이 생기면 여기부터 고친다.

```jsonc
// 요청. 헤더 X-Internal-Token: ${AI_INTERNAL_TOKEN} (없으면 401)
{ "name": "메덩골정원", "region": "경기 양평군" | null }

// 응답 200
{ "draft": {                                   // Pydantic PlaceDraft. 모든 값은 { value, evidenceUrl } 쌍. 근거가 없으면 value: null
    "name":    { "value": "…", "evidenceUrl": "…" },
    "kind":    { "value": "garden", "evidenceUrl": "…" },
    "region":  { … }, "address": { … }, "lat": { … }, "lng": { … },
    "access":  { … }, "visitNote": { … },
    "credits": [ { "makerName": "승효상", "makerKind": "person", "role": "architecture", "evidenceUrl": "…" } ],
    "sources": [ { "title": "…", "url": "…", "type": "article" } ] },
  "usage": { "model": "…", "inputTokens": 0, "outputTokens": 0, "costUsd": 0.0, "latencyMs": 0, "retries": 0, "success": true },
  "incomplete": false }                        // 도구 호출 상한(8회)에 걸려 부분 결과면 true
// 422: 빈 이름. 500: 모델·도구 실패(usage는 그래도 채운다 → Spring이 success=false로 기록)
```

`usage`의 키는 `llm_calls` 컬럼과 1:1이다(`purpose`·`place_id`는 Spring이 채운다). 서버는 `draft`에 `impression`·`sense_*`·`line`·`essay`가 들어 있으면 버린다.

### Spring → Next `POST {FRONTEND_URL}/api/revalidate`

```jsonc
// 헤더 X-Revalidate-Secret: ${REVALIDATE_SECRET} (없거나 다르면 401)
{ "tags": ["place:medongol-garden", "maker:iroje", "browse"] }
// 응답 200 { "revalidated": ["…"] }
// 라우트는 태그마다 revalidateTag(tag, { expire: 0 }) — 다음 요청이 옛 페이지 없이 새로 만든다(DECISIONS 6절)
```

| 태그 | 붙는 페이지 | 보내는 때(SPEC 6절 표) |
| --- | --- | --- |
| `feed` | `/` | 매일 08:00 KST 스케줄러 |
| `browse` | `/places`, `/makers` | 공간 공개·수정·상태 변경 |
| `place:{slug}` | `/places/[slug]`, 그 OG 이미지 | 공간 공개·수정·상태 변경, 크레딧에 있는 만든 사람 수정 |
| `maker:{slug}` | `/makers/[slug]`, 그 OG 이미지 | 만든 사람 수정, 그 사람이 크레딧에 있는 공간의 공개·수정·상태 변경 |
| `sitemap` | `/sitemap.xml` | 공간 처음 공개, 만든 사람 처음 공개 공간 연결 |

호출은 트랜잭션 커밋 뒤에만(`AFTER_COMMIT`). 실패는 로그만. 모든 공개 페이지는 1시간 주기로도 다시 만든다.

### 헬스

`backend`: `GET /actuator/health` · `frontend`: `GET /` 200 · `ai`: `GET /health`(AI 국면 A6에서 compose에 추가). compose healthcheck가 이것들을 본다.

---

## 7. 환경 변수 (이 문서가 전제하는 것)

| 이름 | 어디 | 뜻 |
| --- | --- | --- |
| `FRONTEND_URL` | backend | CORS 출처, 로그인 후 이동, 재생성 호출 대상 |
| `REVALIDATE_SECRET` | backend, frontend | 재생성 호출 비밀 |
| `AI_URL`, `AI_INTERNAL_TOKEN` | backend, ai | 내부 호출 |
| `ADMIN_EMAILS` | backend | 쉼표 구분 |
| `FIXED_NOW` | backend(dev만) | 있으면 `Clock`을 이 시각(ISO-8601)으로 고정 |
| `S3_ENDPOINT`, `S3_REGION`, `S3_BUCKET`, `S3_ACCESS_KEY`, `S3_SECRET_KEY` | backend | R2. 개발은 개발용 버킷, 운영은 운영 버킷. 엔드포인트 `https://<account-id>.r2.cloudflarestorage.com`, region `auto` |
| `PHOTO_PUBLIC_BASE_URL` | backend | 사진 URL 앞부분(개발: 개발 버킷의 r2.dev 주소 / 운영: R2 커스텀 도메인) |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | backend | OAuth(구글만) |
| `NEXT_PUBLIC_API_URL`, `NEXT_PUBLIC_SITE_URL`, `NEXT_PUBLIC_NEWSLETTER_URL` | frontend | API 주소, 메타데이터 기준 URL, 메일리 구독 폼 |
| `TAVILY_API_KEY`, `KAKAO_REST_API_KEY`, LLM 키 | ai | 웹 검색 · 지오코딩 · 모델 |
