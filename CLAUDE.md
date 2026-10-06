# 공간의 지은이 — 작업 기준

공간에서 감동받는 사람들을 위한 서비스. 무엇을 만드는지는 `docs/SPEC.md`, 기술 선택의 근거는 `docs/DECISIONS.md`, 보이는 방식은 `docs/DESIGN.md`, 작업 순서는 `docs/PLAN.md`.
이 파일은 내가 정한 제약과 결정이다. 이 파일과 문서가 어긋나면 작업을 멈추고 묻는다.

## 절대 지킬 것 (SPEC)

- **좋아요·별점·랭킹·인기순·숫자 표시는 만들지 않는다.** 저장 수, 팔로워 수, 조회수도 화면과 공개 API 응답에 넣지 않는다.
- **AI는 감상(`impression`), 감각(`sense_*`), 한 줄(`daily_picks.line`), 에세이를 쓰지 않는다.** AI 초안 응답에 이 필드가 있어도 서버가 버린다. AI가 채운 값에는 근거 URL이 있어야 하고, 없으면 비운다.
- **공간과 만든 사람의 `slug`는 처음 공개한 뒤 바꾸지 않는다.** 공간은 지우지 않는다(폐업은 `closed`). 공개 URL은 한 번 정하면 바꾸지 않는다.
- **상태 전환은 SPEC 3절 표만 허용한다.** published → draft는 없다.
- **"필수"는 공개할 때 검사한다.** DB NOT NULL은 최소로 두고, 공개 시 검증 규칙은 백엔드 `PublishRules` 한 곳과 프론트 Zod 스키마 한 곳에만 있고 둘은 1:1이다.
- **피드 공개 시각은 월~금 08:00 KST.** 공개 여부는 조회 시점에 `pick_date 08:00 KST <= now`로 계산한다. 시간 계산은 서버만 하고 프론트는 서버가 준 값을 그린다. 시각 저장은 UTC(`timestamptz`), KST 변환은 경계 계산에서만.
- **피드 카드의 팔로우 버튼은 아직 팔로우하지 않은 사람에게만** 보인다. 프로필·상세에서는 팔로우/팔로잉 둘 다 보인다.
- **로그인 없이 볼 수 있는 화면**: 피드, 둘러보기, 공간 상세, 만든 사람 프로필. 저장·팔로우·제보를 누를 때만 로그인 시트를 띄우고, 로그인 후 누른 동작을 이어서 완료한다.
- **개발용 로그인(`POST /api/dev/login`), 고정 시계(`FIXED_NOW`), 개발용 시드(`DevSeedRunner`)는 `dev`·`test` 프로필에서만 존재한다.** 운영 프로필에서는 빈이 생기지 않아야 하고, 테스트로 막는다.
- **관리자 API(`/api/admin/**`)는 `role=admin`만.** 관리자는 `ADMIN_EMAILS` 환경 변수로만 정한다.
- **화면 기준 너비 375px.** 공개 화면은 데스크톱에서 최대 480px 가운데. 관리자 화면만 최대 960px.
- **비밀값은 저장소에 넣지 않는다.** `.env.example`에 키 이름만.

## 설계 결정 (DECISIONS에서 채택한 것)

### 저장소

- 모노레포: `backend/`(Spring) · `frontend/`(Next) · `ai/`(Python) · `seed/` · `design/` · `docs/`. 루트 `docker-compose.yml` 한 번으로 `db`·`backend`·`frontend`(AI 국면부터 `ai`)가 뜬다. 사진 저장소 컨테이너는 없다(개발도 R2 개발용 버킷). 포트는 `*_PORT` 환경 변수.

### 백엔드 (Spring Boot 4.1, Java 25 LTS)

- Spring Data JPA. **공개 목록 조회(피드, 둘러보기, 프로필 격자, 내 공간)는 DTO 프로젝션 쿼리**, 목록 API마다 실행 쿼리 수를 테스트로 고정한다. `open-in-view: false`, 트랜잭션은 서비스 계층에서만.
- Flyway로만 스키마를 바꾼다. `ddl-auto: validate`. enum은 `varchar` + `CHECK`.
- 인증: Spring Security `oauth2Login`(MVP는 구글만, 카카오는 공개 후) + Spring Session JDBC. 쿠키 HttpOnly · SameSite=Lax · 운영 Secure. CSRF 켬(쿠키 → 헤더 방식). CORS는 `FRONTEND_URL`만.
- 오류 본문 `{ "message" }`, 공개 검증 실패는 `{ "message", "fieldErrors" }`. 상태 코드: 검증 400 · 비로그인 401 · 권한 403 · 없음 404 · 규칙 위반(전환표 밖, slug 잠금, 중복 편성, 폐업 저장) 409 · 제보 한도 429 · AI 시간 초과 504.
- 시간은 `Clock` 빈으로만 읽는다(`LocalDateTime.now()` 직접 호출 금지). 테스트는 고정 시계.
- 사진: S3 호환 API만 쓴다(개발·운영 모두 R2, 버킷만 다름). 모든 사진에 `credit`, `own`이 아니면 `source_url` 필수. 허락 절차 없음. 서버는 서명 URL만 발급하고 파일을 받지 않는다. `image/webp`만.
- 페이지 재생성 호출은 트랜잭션 커밋 뒤(`AFTER_COMMIT`)에만 보낸다. 실패는 로그만.
- AI 서비스는 내부망으로만 호출한다. `llm_calls`는 Spring이 쓴다(AI 서비스는 DB를 만지지 않는다).
- 패키지: `place/ maker/ feed/ user/ save/ follow/ suggestion/ admin/ upload/ ai/ common/`. 계층은 클래스 접미사(Controller · Service · Repository).

### 프론트엔드 (Next.js 16 App Router, TypeScript)

- 공개 화면(피드·상세·프로필·둘러보기)은 서버 컴포넌트 + 태그 캐시. `generateStaticParams`는 빈 배열, `dynamicParams: true`, 공개·수정 시 Spring이 프론트의 재생성 라우트를 불러 태그를 무효화한다. **Next 16에서 `revalidateTag`는 두 번째 인자(cacheLife)가 필수이고 기본 권장값은 "옛 페이지를 먼저 보여주고 뒤에서 갱신"이다. 관리자가 공개한 즉시 새 내용이 보여야 하므로, 즉시 반영되는 방법을 F1에서 공식 문서로 확인하고 DECISIONS에 기록한다.** `updateTag`는 Server Actions 전용이라 이 구조에서는 못 쓴다. **빌드는 백엔드 없이 통과해야 한다.**
- 공개 화면 위의 사용자별 값(저장·팔로우 상태)은 클라이언트 섬에서 `/api/me/state`로 따로 받는다.
- 로그인 후 화면·관리자 화면은 클라이언트 컴포넌트 + TanStack Query. 저장·팔로우는 낙관적 업데이트 + 실패 롤백 토스트.
- 스타일: CSS Modules + `tokens.css`의 변수와 글자 클래스만. **색·간격·글자 크기를 숫자로 직접 쓰지 않는다**(토큰에 없으면 DESIGN.md에 먼저 추가). 컴포넌트 라이브러리·Tailwind 없음.
- 폴더: `app/`은 라우트만(page.tsx는 조립만), `features/<기능>/`(api.ts · queries.ts · types.ts · components/), `shared/`(api · ui · lib). 두 기능 이상이 쓰면 shared.
- 피드 세로 넘기기·사진 가로 넘기기는 CSS scroll-snap. 라이브러리 없음.
- 관리자 사진은 브라우저에서 WebP 1600·800으로 변환 후 서명 URL로 직접 업로드.

### AI 초안 서비스 (Python 3.14, uv, FastAPI)

- Tool Calling 루프는 **프레임워크 없이 직접 구현**한다(A3). LangGraph 이식은 공개 후(A9).
- 출력은 Pydantic `PlaceDraft`, 모든 값에 `evidence_url`. 검증 실패 시 오류를 모델에 돌려 최대 2회 재시도. 도구 호출 최대 8회.
- LLM 호출은 얇은 인터페이스 하나 뒤에. 가격표는 상수 + 출처 주석.
- 외부 도구: Tavily 웹 검색 · 카카오 로컬 API · httpx 페이지 읽기. 타임아웃 10초. **검색 결과는 저장하지 않고 그 호출 안에서만 쓴다.**
- `POST /drafts`는 `X-Internal-Token` 없으면 401. 응답에 사용량을 담는다.
- 평가는 `ai/eval/`의 정답 20곳으로만 말한다. 지표를 바꾸거나 정답을 고치면 결과 파일에 이유를 남긴다.

## 디자인 판단 (DESIGN.md 기준)

- 고요하고 섬세하게: 종이색 바탕, 먹색 글자, 포인트는 흙색(`--color-earth`) 하나. 헤어라인 1px, 모서리 2px, 그림자·그라데이션 버튼 없음(사진 위 그림자만 예외).
- 제목 Noto Serif KR, 본문·라벨 IBM Plex Sans KR. 글자 단계는 `tokens.css`의 8개 클래스만.
- `--color-ink-tertiary`(대비 2.4:1)는 10~12px 라벨과 보조 정보에만. 읽어야 하는 문장에는 쓰지 않는다.
- 하단 탭: 오늘 · 둘러보기 · 내 공간. 아이콘 없음. 내 공간 옆 점은 팔로우한 사람의 새 공간이 있을 때만.
- 사진 자리가 비면 `--color-photo-placeholder`. 격자 캡션 아래에는 옅은 그림자.
- 화면 구현 후 DESIGN.md 5절 수치로 대조한다. 피그마는 MCP 한도 때문에 기준으로 쓰지 않는다(원본 보관용).

## 테스트

- 백엔드: `@SpringBootTest` + MockMvc + Testcontainers PostgreSQL. **테스트는 시드에 기대지 않고 필요한 데이터를 직접 만든다.** 최소 케이스 — 공개 시 검증(필수 누락 400 필드) · 상태 전환표 밖 409 · 공개 후 slug 변경 409 · 관리자 API 일반 사용자 403 · 운영 프로필에 개발용 로그인 빈 없음 · CSRF 없는 POST 403 · 저장 want↔been 전환과 해제 · 다른 사용자 데이터 비노출 · 팔로우한 사람의 공간 중복 제거와 `isNew`·탭 점 · 피드 경계(수 07:59 / 08:00, 월 07:59 지난주, 금 08:00 뒤 다음 공개 시각) · 편성 규칙(주말 400, 중복 409, 오늘 08:00 뒤 409) · 목록 API 쿼리 수 · 개발용 시드는 dev 프로필에서만 · 재생성 호출은 커밋 뒤에만 · 탈퇴 시 데이터 처리.
- 프론트: 순수 함수(Zod 공개 스키마, 피드 마지막 화면 문구, 저장 상태 전환)만 Vitest. 화면은 DESIGN.md 5절과 스크린샷 비교.
- AI: 도구·파싱·재시도는 pytest(외부 호출은 녹화 응답). 품질은 평가 러너로만 말한다. 평가는 CI에서 자동으로 돌리지 않는다(비용).

## 작업 방식

- `docs/PLAN.md`의 단위를 위에서부터 하나씩. 한 번에 한 단위만. 끝나면 멈추고 테스트 결과, diff 요약, **실제 걸린 시간**을 보여준다. 내가 PLAN의 `실제` 칸을 채운다.
- 각 국면(S·A·B·F·M·D)의 첫 단위는 구현 전에 접근 방식을 먼저 설명하고 승인을 받는다. 나머지 단위는 바로 구현.
- 단위가 예상 시간의 1.5배를 넘기면 멈추고 알린다. 원인과 함께 "계속 / 범위 줄이기 / PLAN의 줄이는 순서 적용" 중 하나를 제안한다.
- 파일 편집·테스트·빌드·curl은 자유. **새 의존성 추가와 이 파일에 없는 설계 판단은 하기 전에 묻는다.** 커밋·푸시는 하지 않는다(내가 한다).
- 확인은 실제로 실행해서 한다. `./gradlew test`, `npm test`, `uv run pytest`, curl, `docker compose up --build`.
- 설명 못 할 추상화·유틸·주석 금지. 장황한 doc 주석 금지. 코드는 읽는 사람이 이해할 만큼만.
- 새 파일·새 의존성에는 이유를 한 줄 말한다.
- Spring Boot 4.1(Jackson 3, Spring Security 7, 모듈화된 스타터) · Spring Session · Next.js 16(캐시 API 변경) · MCP SDK · 외부 API(Tavily·카카오·구글·LLM)처럼 버전이나 약관에 따라 달라지는 것은 추측하지 말고 공식 문서를 확인한 뒤 출처를 말한다.
- 한 단위에서 2시간 넘게 막히면 더 단순한 안을 제안하고, 그 판단을 `docs/DECISIONS.md` 끝에 한 줄 추가한다.
- SPEC·DESIGN과 다르게 구현해야 할 이유가 생기면 코드보다 문서를 먼저 고치자고 말한다.
- 내가 "이 줄 왜 필요해", "Spring이 여기서 자동으로 하는 게 뭐야"라고 물으면 짧고 정확하게 답한다. 그 답은 `docs/NOTES.md`(커밋 안 함)에 내가 적는다.
- README의 빈 절은 마지막(D6)에 내가 쓴다. 초안을 요청하면 파일·함수명을 짚어 짧게, 내 말투로 고칠 수 있게.
