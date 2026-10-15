# 개발 규칙

이 저장소에서 코드를 고칠 때 지키는 규칙입니다. 무엇을 만드는지는 [docs/SPEC.md](docs/SPEC.md), 기술 선택의 이유는 [docs/DECISIONS.md](docs/DECISIONS.md), 보이는 방식은 [docs/DESIGN.md](docs/DESIGN.md), API 모양은 [docs/API.md](docs/API.md)에 있습니다.

## 제품 규칙

제품 규칙의 기준은 [SPEC.md 1절 "지키는 규칙"](docs/SPEC.md#1-지키는-규칙) 하나입니다. 숫자 표시 금지, slug 불변, 상태 전환표, 공개할 때 검사, 08:00 KST 공개, 로그인 없이 볼 수 있는 화면, 개발용 기능의 프로필 제한, 관리자 권한, 화면 너비가 여기에 있습니다. 코드와 SPEC이 어긋나면 코드를 바꾸기 전에 SPEC을 먼저 고칩니다.

## 실행과 확인

```bash
cp .env.example .env            # POSTGRES_PASSWORD만 채우면 뜬다
docker compose up --build       # db · backend(dev 프로필, 가짜 시드) · frontend

cd backend && ./gradlew test    # Testcontainers PostgreSQL. Docker 필요
cd frontend && npm test         # Vitest
cd frontend && npm run typecheck && npm run lint
```

- 로그인·관리자 화면은 `docker compose up -d db backend` 뒤 `cd frontend && API_URL=http://localhost:8080 npm run dev`로 봅니다. 개발 서버에서만 개발용 로그인 버튼이 보이고, `.env`의 `ADMIN_EMAILS`에 넣은 이메일이 관리자가 됩니다.
- 확인은 실제로 실행해서 합니다: 테스트, curl, `docker compose up --build`.
- CI는 PR마다 바뀐 경로에 따라 프론트(타입 검사 · lint · Vitest)와 백엔드(`./gradlew test`)를 돌립니다.

## 백엔드 (Spring Boot 4.1, Java 25)

- 패키지는 기능별입니다: `place/ maker/ feed/ user/ save/ follow/ suggestion/ admin/ upload/ ai/ common/`. 계층은 클래스 접미사(Controller · Service · Repository)로 나눕니다.
- 공개 목록 조회(피드, 둘러보기, 프로필 격자, 내 공간)는 DTO 프로젝션 쿼리로 쓰고, 목록 API마다 실행 쿼리 수를 테스트로 고정합니다. `open-in-view: false`, 트랜잭션은 서비스 계층에서만 엽니다.
- 스키마는 Flyway 마이그레이션으로만 바꿉니다(`ddl-auto: validate`). enum은 `varchar` + `CHECK`입니다.
- 공개 시 검증 규칙은 백엔드 `PublishRules`와 프론트 Zod 스키마(`features/admin/placeSchema.ts`) 두 곳에만 있고, 키·조건·문구가 1:1입니다. 한쪽을 바꾸면 다른 쪽도 바꿉니다.
- 시간은 `Clock` 빈으로만 읽습니다(`LocalDateTime.now()` 직접 호출 금지). 시각은 UTC(`timestamptz`)로 저장하고, KST는 08:00 경계 계산에서만 씁니다.
- 저장·팔로우는 네이티브 upsert(`INSERT … ON CONFLICT`)로만 씁니다. id를 직접 넣는 `DailyPick`은 `Persistable`로 SELECT 없이 INSERT합니다.
- 오류 본문은 `{ "message" }`, 공개 검증 실패는 `{ "message", "fieldErrors" }`입니다. 상태 코드: 검증 400 · 비로그인 401 · 권한 403 · 없음 404 · 규칙 위반(전환표 밖, slug 잠금, 중복 편성, 폐업 공간 저장) 409 · 제보 한도 429.
- 개발용 로그인(`POST /api/dev/login`)은 `dev`·`test` 프로필, 고정 시계(`FIXED_NOW`)와 개발용 시드(`DevSeedRunner`)는 `dev` 프로필에만 있습니다. 운영 프로필에 이 빈들이 없다는 것을 테스트(`ProdProfileTest`)로 막습니다.
- 페이지 재생성 호출은 트랜잭션 커밋 뒤(`AFTER_COMMIT`)에만 보내고, 실패는 로그만 남깁니다.
- 사진은 S3 호환 API(R2)만 씁니다. 서버는 서명 URL만 발급하고 파일을 받지 않습니다. `image/webp`만 받습니다.

## 프론트엔드 (Next.js 16 App Router, TypeScript)

- 브라우저는 백엔드를 직접 부르지 않습니다. Next rewrites가 `/api/**`·`/oauth2/**`·`/login/oauth2/**`를 백엔드로 넘깁니다(같은 출처, CORS 없음).
- 공개 화면은 서버 컴포넌트 + 태그 캐시입니다. 상세 페이지는 빈 `generateStaticParams` + `dynamicParams = true`라 **빌드는 백엔드 없이 통과해야 합니다.** 캐시 무효화는 `revalidateTag(tag, { expire: 0 })`입니다.
- 공개 화면 위의 사용자별 값(저장·팔로우 상태)은 클라이언트 섬에서 `/api/me/state`로 따로 받습니다.
- 로그인 뒤 화면과 관리자 화면은 클라이언트 컴포넌트 + TanStack Query입니다. 저장·팔로우는 낙관적 업데이트 + 실패하면 되돌리고 토스트.
- 폴더: `app/`은 라우트만(`page.tsx`는 조립만), `features/<기능>/`(`api.ts` · `queries.ts` · `types.ts` · `components/`), 두 기능 이상이 쓰는 것은 `shared/`(`api` · `ui` · `lib`).
- 스타일은 CSS Modules + `frontend/app/tokens.css`의 변수와 글자 클래스만 씁니다. 색·간격·글자 크기를 숫자로 직접 쓰지 않고, 토큰에 없으면 DESIGN.md와 `tokens.css`에 먼저 추가합니다. 컴포넌트 라이브러리와 Tailwind는 쓰지 않습니다.
- 피드 세로 넘기기와 사진 가로 넘기기는 CSS scroll-snap으로 만듭니다.
- Next.js 16은 캐시 API가 이전 버전과 다릅니다. 쓰기 전에 `frontend/node_modules/next/dist/docs/`의 해당 문서를 확인합니다(`frontend/AGENTS.md`).

## 테스트

- 백엔드: `@SpringBootTest` + MockMvc + Testcontainers PostgreSQL. 테스트는 시드에 기대지 않고 필요한 데이터를 직접 만듭니다. 고정하는 항목은 [DECISIONS.md 10절](docs/DECISIONS.md#10-ci--테스트--협업)에 있습니다.
- 동시성 테스트는 보호 장치(upsert, 잠금)를 빼고 돌려 실패하는 것까지 확인합니다([TROUBLESHOOTING.md 1](docs/TROUBLESHOOTING.md#1-동시-요청에서-중복-키-오류와-한도-초과)).
- 프론트: 순수 함수(공개 검증 스키마, 피드 마지막 화면 문구, 저장 상태 전환 등)만 Vitest로 확인합니다.

## 코드

- 주석은 이유를 그 자리에서 한 줄로 적습니다. 문서의 절 번호는 코드에 쓰지 않습니다(바뀌면 틀린 주석이 됩니다). 설명하지 못할 추상화·유틸·긴 doc 주석은 넣지 않습니다.
- 새 의존성을 넣을 때는 이유를 남깁니다.
- 버전·약관에 따라 달라지는 것(Spring Boot 4.1, Spring Security 7, Next.js 16, 외부 API)은 공식 문서로 확인하고, 결정이면 DECISIONS.md 부록에 출처를 남깁니다.
- 비밀값은 저장소에 넣지 않습니다. `.env.example`에는 키 이름과 설명만 둡니다.
