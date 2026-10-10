# 공간의 지은이 — 기술 선택 기록 (DECISIONS v0.3)

> 코드를 쓰기 전에 내린 결정들. 각 항목 끝의 "채택 / 포기한 것"이 결론이다 (2026-10-06, 9절만 보류).
> 근거는 `SPEC.md`(무엇을 만드는가)에서 끌어온다.

## 전제

- **두 목표는 같은 무게다.** ① 공간에서 감동받는 사람을 모으는 서비스, ② 풀스택 역량을 보여주고 만들면서 배우는 포트폴리오. 서비스의 조용한 결을 해치지 않는 한 ②에 필요한 기능은 넣는다.
- **범위**: SPEC의 MVP. 공개 화면(피드, 둘러보기, 공간 상세, 만든 사람 프로필), 로그인 후 기능(저장, 팔로우, 제보), 관리자 화면(등록 폼, 편성, 제보 검토, AI 호출 기록), AI 초안 서비스.
- **나**: React·Next·TS 실무 경험 있음. Spring Boot는 JdbcTemplate·Flyway·Testcontainers 조합으로 한 번 써 봤다. Python은 기본.
- **원칙**: 저장소의 모든 줄을 설명할 수 있어야 한다. 설명할 수 없는 복잡도는 넣지 않는다.

---

## 요약

| 레이어 | 채택 |
| --- | --- |
| 프론트엔드 | Next.js 16 (App Router) + TypeScript, CSS Modules, TanStack Query |
| 백엔드 | Spring Boot 4.1 (Java 25 LTS) REST API |
| 데이터 접근 | Spring Data JPA + 목록 조회는 DTO 프로젝션 쿼리 |
| DB | PostgreSQL 18 + Flyway |
| 인증·권한 | Spring Security OAuth2 Client(구글, 카카오는 공개 후) + HttpOnly 세션, user/admin 역할 |
| 콘텐츠 | DB + 관리자 등록 폼, 공개 시 검증 |
| 사진 | Cloudflare R2(개발·운영), 서명된 URL로 브라우저 직접 업로드 |
| 예약 공개 | 조회 시점 계산 + 08:00 스케줄러가 홈 재생성 |
| AI 초안 | Python 3.14 + FastAPI, Tavily 검색, Tool Calling 직접 구현 → LangGraph 이식, MCP 서버 |
| 배포 | 프론트 Vercel, 백엔드·DB·AI 서비스는 서버 한 대에 Docker Compose (호스팅 보류) |
| CI | GitHub Actions: 테스트, 타입 검사 |

---

## 버전 기준 (2026-10-06 공식 문서로 확인)

아래 표는 2026-10-06에 공식 문서로 확인한 버전과 설정이다. Spring Boot 3.5는 오픈소스 지원이 끝나 쓰지 않는다. 패치 버전은 프로젝트를 만드는 날 한 번 더 본다.

| 대상 | 버전 | 확인한 것 | 출처 |
| --- | --- | --- | --- |
| Spring Boot | **4.1.1** (2026-08-20) | OSS 지원: 3.5 → 2026-06-30 종료, 4.0 → 2026-12-31, 4.1 → 2027-07-31. Java 17~26 지원. Gradle 8.14+ 또는 9.x. 관리 버전: Spring Framework 7.0, Security 7.1, Session 4.1, Hibernate ORM 7.4.5, Flyway 12.4.0, Testcontainers 2.0.5 | [릴리스](https://github.com/spring-projects/spring-boot/releases) · [지원 기간](https://api.spring.io/projects/spring-boot/generations) · [시스템 요구사항](https://docs.spring.io/spring-boot/system-requirements.html) · [관리 좌표](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html) · [4.1 릴리스 노트](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.1-Release-Notes) |
| Boot 4 스타터 이름 | — | `spring-boot-starter-data-jpa` · `spring-boot-starter-flyway`(4.0부터 필요) · `spring-boot-starter-session-jdbc` · **`spring-boot-starter-security-oauth2-client`**(`spring-boot-starter-oauth2-client`는 deprecated) · `spring-boot-starter-security` · `spring-boot-starter-validation` · `spring-boot-starter-actuator` · 테스트 `spring-boot-testcontainers`. Jackson 3는 `tools.jackson`, `JsonMapperBuilderCustomizer` | [4.0 마이그레이션 가이드](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) |
| Spring Session JDBC | 4.1 | 스키마 파일 `org/springframework/session/jdbc/schema-postgresql.sql`을 Flyway로 복사, `spring.session.jdbc.initialize-schema=never`(기본 `embedded`) | [Session JDBC](https://docs.spring.io/spring-session/reference/configuration/jdbc.html) · [schema-postgresql.sql](https://github.com/spring-projects/spring-session/blob/main/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-postgresql.sql) |
| Spring Security | 7.1 | SPA용 CSRF는 **`http.csrf(csrf -> csrf.spa())`**(7.0부터; 쿠키 저장소 + 평문 토큰 핸들러). 6.x의 손으로 쓴 `SpaCsrfTokenRequestHandler` 예제는 문서에서 사라짐. 로그인·로그아웃 성공 시 CSRF 쿠키가 지워진다. **`spa()`는 토큰을 쓸 때만 쿠키를 만들어 GET 응답에는 쿠키가 없다**(7.1.1 jar 확인) → 토큰을 한 번 읽는 필터(`CsrfCookieFilter`)를 `CsrfFilter` 뒤에 둔다. 테스트에서 Spring Security Test의 `csrf()`는 CSRF 저장소를 세션 기반으로 바꿔 끼우고 되돌리지 않으므로 쿠키+헤더를 직접 싣는다. `oauth2Login` 커스텀 provider 속성(`provider.{id}.authorization-uri/token-uri/user-info-uri/user-name-attribute`)은 그대로 | [CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) · [CsrfConfigurer.spa()](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/config/annotation/web/configurers/CsrfConfigurer.html) · [OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html) |
| Testcontainers | 2.0.5 | 아티팩트 `org.testcontainers:testcontainers-postgresql`, 패키지 `org.testcontainers.postgresql.PostgreSQLContainer`, `@ServiceConnection` | [Boot Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html) |
| Hibernate ORM | 7.4.5 | `Statistics.getPrepareStatementCount()` · `getQueryExecutionCount()` 있음 → 목록 API 쿼리 수 테스트 | [Statistics javadoc](https://docs.hibernate.org/orm/7.4/javadocs/org/hibernate/stat/Statistics.html) |
| Java | **25 LTS** | Boot 4.1.1 지원 범위 안 | 위 시스템 요구사항 |
| PostgreSQL | **18.6** | 2030-11까지 지원. Docker `postgres:18`. Flyway 12.4가 18을 "Verified" | [버전 정책](https://www.postgresql.org/support/versioning/) · [Docker Hub](https://hub.docker.com/_/postgres) · [Flyway PostgreSQL](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database) |
| Next.js | **16.3.8** (2026-09-30) | Node ≥ 20.9. `revalidateTag(tag, profile)` 두 번째 인자 필수 — 프로필(`'max'` 등)은 다음 요청에 옛 페이지를 먼저 주고(stale-while-revalidate), **`{ expire: 0 }`만 다음 요청을 막고 새로 만든다**. `updateTag`는 Server Action 전용. `generateStaticParams` 빈 배열 + `dynamicParams`는 그대로 유효하되 **`cacheComponents`를 켜면 빈 배열이 빌드 오류** → 끈다. `fetch(…, { next: { tags, revalidate } })` 유지. `next lint` 제거 → `eslint.config.mjs`. `create-next-app --no-tailwind`. `opengraph-image`의 `ImageResponse` 번들 500KB 제한 | [릴리스](https://github.com/vercel/next.js/releases) · [revalidateTag](https://nextjs.org/docs/app/api-reference/functions/revalidateTag) · [updateTag](https://nextjs.org/docs/app/api-reference/functions/updateTag) · [generateStaticParams](https://nextjs.org/docs/app/api-reference/functions/generate-static-params) · [cacheComponents](https://nextjs.org/docs/app/api-reference/config/next-config-js/cacheComponents) · [캐시(이전 모델)](https://nextjs.org/docs/app/guides/caching-without-cache-components) · [ESLint](https://nextjs.org/docs/app/api-reference/config/eslint) · [ImageResponse](https://nextjs.org/docs/app/api-reference/functions/image-response) |
| Node.js | **24 LTS** (v24.21) | 26은 2026-10-28에 LTS. 공개 전 Vercel 지원을 보고 D1에서 결정 | [Node 릴리스](https://nodejs.org/en/about/previous-releases) |
| Vercel Hobby | — | 이미지 변환 월 5,000회, `remotePatterns` 원격 이미지도 횟수에 든다, `<img>`·`unoptimized`는 안 든다. 비상업 전용 | [이미지 한도](https://vercel.com/docs/image-optimization/limits-and-pricing) · [Hobby](https://vercel.com/docs/plans/hobby) · [공정 사용](https://vercel.com/docs/limits/fair-use-guidelines#commercial-usage) |
| Python | **3.14.8** (2026-09-30) | 3.15는 rc3. FastAPI 0.142.2 · Pydantic 2.13.5 · `mcp` 2.3.0 · anthropic 1.11 · openai 3.24는 3.14 분류자 있음. **LangGraph 1.2.13은 3.13까지만 적혀 있음**(A9에서 확인, 안 되면 `ai/`만 3.13). httpx 0.28.1 | [python.org](https://www.python.org/downloads/) · [fastapi](https://pypi.org/project/fastapi/) · [pydantic](https://pypi.org/project/pydantic/) · [mcp](https://pypi.org/project/mcp/) · [langgraph](https://pypi.org/project/langgraph/) |
| MCP Python SDK | **2.x** | v2에서 `FastMCP` → **`MCPServer`**, 전송 인자는 `run()`으로. Claude Desktop 설정: `~/Library/Application Support/Claude/claude_desktop_config.json` | [마이그레이션](https://github.com/modelcontextprotocol/python-sdk/blob/main/docs/migration.md) · [서버 만들기](https://modelcontextprotocol.io/docs/develop/build-server) · [Desktop 설정](https://modelcontextprotocol.io/quickstart/user) |
| uv | 0.12.x | `uv python install 3.14` 지원. 로컬 설치본은 0.6.11이라 S1 전에 올린다 | [uv Python 버전](https://docs.astral.sh/uv/concepts/python-versions/) |
| Cloudflare R2 · AWS SDK for Java v2 | SDK **2.55.14** (2026-10-09, BOM) | 서명 URL은 PUT 지원, 최대 7일, **`*.r2.cloudflarestorage.com`에서만**(커스텀 도메인 불가). `Content-Type`을 넣어 서명하면 브라우저가 같은 값을 보내야 한다(다르면 403). `S3Client`: `Region.of("auto")`, `pathStyleAccessEnabled(true)`, **`chunkedEncodingEnabled(false)`**(없으면 403) + `requestChecksumCalculation/ResponseChecksumValidation = WHEN_REQUIRED`(SDK 2.30+ 기본 체크섬). **`S3Presigner.Builder`에는 체크섬 옵션이 없고 "chunkedEncodingEnabled·checksumValidationEnabled는 서명 요청에 적용되지 않는다"**(javadoc) → 프리사이너는 path style만. 브라우저 PUT에는 CORS 정책 필요(`PUT`, `Content-Type`, `ETag`). r2.dev는 개발 전용·속도 제한, 운영은 커스텀 도메인(캐시 가능). 무료: 10GB, Class A 100만, Class B 1,000만/월, 송신 무료 | [서명 URL](https://developers.cloudflare.com/r2/api/s3/presigned-urls/) · [Java SDK 예제](https://developers.cloudflare.com/r2/examples/aws/aws-sdk-java/) · [S3 API 호환](https://developers.cloudflare.com/r2/api/s3/api/) · [AWS 체크섬](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/s3-checksums.html) · [S3Presigner.Builder](https://docs.aws.amazon.com/java/api/latest/software/amazon/awssdk/services/s3/presigner/S3Presigner.Builder.html) · [BOM](https://repo1.maven.org/maven2/software/amazon/awssdk/bom/maven-metadata.xml) · [CORS](https://developers.cloudflare.com/r2/buckets/cors/) · [공개 버킷](https://developers.cloudflare.com/r2/buckets/public-buckets/) · [가격](https://developers.cloudflare.com/r2/pricing/) |
| MinIO | **쓰지 않음** | 커뮤니티 저장소 2026-04-24 보관(archived), "소스 코드로만 배포", Docker Hub `minio/minio` 404. → R2 개발용 버킷(12-2절 채택) | [github.com/minio/minio](https://github.com/minio/minio) |
| 카카오 로그인 | 공개 후 | `kauth.kakao.com/oauth/authorize` · `/oauth/token`(본문에 `client_secret`) · `kapi.kakao.com/v2/user/me`. `account_email`은 **비즈 앱·테스트 앱**에서만. 개인 개발자 비즈 앱 전환 조건: 앱 소유자 본인인증 + 카카오비즈니스 통합 서비스 약관 동의 | [REST API](https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api) · [동의항목](https://developers.kakao.com/docs/latest/ko/kakaologin/utilize) · [앱 설정](https://developers.kakao.com/docs/latest/ko/app-setting/app) |
| 카카오 로컬 API | — | 주소 검색·키워드 장소 검색 각 **10만 건/일**, 전체 월 300만 건 무료(개발자 계정의 첫 앱만). 로그인과 같은 앱에서 토글 | [쿼터](https://developers.kakao.com/docs/latest/ko/getting-started/quota) · [로컬 공통](https://developers.kakao.com/docs/latest/ko/local/common) |
| 구글 OAuth | — | scope `openid email profile`. 테스트 모드는 사용자 100명·동의 7일 만료 → 앱 게시. 이 세 scope만 쓰면 검증 없이 게시 가능 | [OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect) · [게시 상태](https://support.google.com/cloud/answer/15549945) |
| 네이버 검색 API | **쓰지 않음** | 2026-07-30 developers.naver.com 신규 신청 종료(NAVER API HUB로 이전). 약관 2.3: 복사·저장·캐싱 금지, **"데이터를 AI에 입력하거나 학습, 개선, 평가 및 노출 등에 활용하는 행위" 금지**. → Tavily(12-1절 채택) | [이용약관](https://developers.naver.com/products/terms) · [검색 API 소개](https://developers.naver.com/products/service-api/search/search.md) |

Spring Boot 3.x와 달라지는 것: Jackson 3(패키지 `tools.jackson`), Spring Security 7(`csrf.spa()`), 의존성이 작은 모듈로 나뉜 스타터, Testcontainers 2, null 표기(JSpecify). 처음부터 4.1로 시작하므로 마이그레이션 비용은 없다.

---

## 1. 백엔드 — Supabase만 vs Spring Boot vs NestJS

### 선택지

**A. Supabase만 (SPEC v0.1 안)**

- 장점: 서버 코드가 거의 없다. 로그인·권한·DB가 한 번에 해결된다. 무료.
- 단점: 보여주려는 REST API 설계, 서버 권한 검사, 트랜잭션, 쿼리 최적화를 보여줄 코드가 남지 않는다. 권한 로직이 SQL 정책(RLS) 안에 숨는다. 무료 프로젝트는 7일 요청이 없으면 일시 정지된다.

**B. Spring Boot**

- 장점: 국내 백엔드의 기본 스택이다. 기본 조합(JdbcTemplate·Flyway·Testcontainers)은 써 봤기 때문에, 이번에 새로 다루는 것(JPA, OAuth2, 역할 권한, 파일 업로드, 스케줄링, 운영 배포)에 집중할 수 있다.
- 단점: 서버를 상시 띄워야 해서 비용과 운영 부담이 생긴다.

**C. NestJS**

- 장점: TS 한 언어로 끝난다.
- 단점: 실무·과제 경험이 없다. 국내에서는 Spring 생태계가 더 넓다.

### 채택 / 포기한 것

- **채택: B. Spring Boot 4.1, Java 25(현재 LTS).** 3.5는 2026-06-30에 오픈소스 지원이 끝났고 4.0도 2026-12에 끝나므로 새 프로젝트는 4.1로 시작한다(아래 "버전 기준").
- **포기한 것**: Supabase의 무료·무운영. TS 한 언어로 끝나는 단순함.

---

## 2. 데이터 접근 — JdbcTemplate vs JPA

### 선택지

**A. JdbcTemplate (써 본 방식)**

- 장점: SQL이 그대로 보인다. 이미 해 봤다.
- 단점: 같은 걸 한 번 더 하는 것이다. 국내 Spring 생태계는 JPA가 기본이다(영속성 컨텍스트, N+1, 지연 로딩).

**B. Spring Data JPA**

- 장점: 관리자 화면의 CRUD(공간, 크레딧, 사진, 출처, 편성)가 많아 보일러플레이트가 크게 줄어든다. "두 방식을 다 써 봤고, 이번엔 왜 JPA였나"를 설명할 수 있다.
- 단점: 암묵적 동작이 많다. 피드·격자·내 공간처럼 목록 화면이 많아 N+1이 생기기 쉽다.

### 채택 / 포기한 것

- **채택: B. JPA.** 단, 공개 목록 조회(피드, 둘러보기, 프로필 격자, 내 공간)는 엔티티 그래프 대신 **DTO 프로젝션 쿼리**로 쓰고, 목록 API마다 실행되는 쿼리 수를 테스트로 고정한다. 인덱스는 `EXPLAIN ANALYZE` 전후를 README에 남긴다.
- **2026-10-06(B4) 둘러보기 인덱스와 커서**: B1의 `places(status, published_at desc, id desc)`는 쓰이지 않았다. 조건이 `status in ('published','paused')` 두 값이라 이 인덱스 순서로는 전체 `published_at desc` 순서를 만들 수 없어서, 조건에 맞는 행을 모두 읽고 정렬했다. **채택: 부분 인덱스 `(published_at desc, id desc) where status in ('published','paused') and slug is not null`(V3), 커서 조건은 행 비교 `(published_at, id) < (?, ?)`.** `or`로 쓴 커서 조건(`a < ? or (a = ? and id < ?)`)은 인덱스 범위 조건이 되지 못하고 필터로 걸려 앞쪽 행을 전부 읽고 버린다. 측정(공간 2만 곳 중 목록 대상 1만 6천, 25개씩, 같은 쿼리 3번 중 마지막):

  | 경우 | 전: 옛 인덱스 | 후: 부분 인덱스 |
  | --- | --- | --- |
  | 첫 페이지 | 7.673 ms (Seq Scan + 정렬) | 0.044 ms (Index Scan, 25행에서 멈춤) |
  | 8,000번째 뒤 페이지, `or` 조건 | 4.724 ms (Bitmap Heap Scan) | 0.846 ms (Index Scan, 필터로 8,001행 버림) |
  | 8,000번째 뒤 페이지, 행 비교 | 4.376 ms (Bitmap Heap Scan) | 0.061 ms (Index Scan, 범위 조건) |

  **포기한 것**: 상태 값이 바뀌면(예: 목록에 새 상태 추가) 인덱스 조건도 마이그레이션으로 같이 바꿔야 한다.
  상태가 바인드 파라미터라 generic 계획에서는 부분 인덱스를 못 쓴다(기본 `auto`에서는 custom 계획 유지, 확인함: 준비된 문장 8번 실행 뒤 generic 0 · custom 9, 0.008 ms / `force_generic_plan`이면 Seq Scan + 정렬 3.209 ms).
- `open-in-view`는 끈다. 트랜잭션 경계는 서비스 계층에서만 연다.
- **포기한 것**: SQL이 전부 눈에 보이는 투명함. 일정이 밀리면 A로 되돌리고, 그 판단을 이 파일 끝에 한 줄 남긴다.

---

## 3. DB — PostgreSQL 18 + Flyway

- 써 본 조합이라 설명 비용이 없다.
- 스키마는 Flyway 마이그레이션 파일로만 바꾼다. JPA의 `ddl-auto`는 `validate`.
- 테이블은 SPEC 3절의 12개 + Spring Session 테이블 2개.
- **필수 항목은 DB 제약이 아니라 공개 전환 때 서비스 계층에서 검사**한다(SPEC 3절). 제보·작성 중 상태에서는 빈칸이 정상이기 때문이다. 대신 형식(길이, enum, unique)은 DB 제약으로 둔다.
- 개발용 시드: `dev` 프로필에서만 등록되는 `ApplicationRunner`가 서버 시작 때 `makers`가 비어 있으면 `seed/seed.json`(가짜 5곳)을 한 트랜잭션으로 적재한다. 행이 있으면 건너뛴다.
- **2026-10-06(B1) 추가**: `created_at`·`updated_at`은 Spring Data JPA Auditing으로 채우되 `DateTimeProvider`가 `Clock` 빈을 읽는다(고정 시계 테스트와 일관). 탈퇴 규칙(SPEC 3절)은 FK 동작으로도 건다 — `saves`·`follows`는 `ON DELETE CASCADE`, `places.suggested_by`·`credit_suggestions.user_id`는 `ON DELETE SET NULL`. `Place`의 자식 목록(크레딧·출처·사진·임베드)은 **둘 이상을 `join fetch`하지 않는다**(카테시안 곱·`MultipleBagFetchException`); `hibernate.default_batch_fetch_size=100`으로 목록마다 IN 쿼리 한 번씩. `Save`·`Follow`·`DailyPick`처럼 id를 직접 넣는 엔티티는 `Persistable`을 구현해 `save()`가 SELECT 없이 INSERT한다.
- **2026-10-06(B4 리뷰) 추가**: "확인된 크레딧은 출처가 있다"(`place_credits`: `NOT verified OR source_url IS NOT NULL`)는 공개 여부와 상관없이 항상 참이어야 하는 형식 규칙이라 DB `CHECK`로도 둔다(V3). 공개할 때만 따지는 규칙(출처 있는 크레딧 1개 이상, 사진 출처 등)은 계속 `PublishRules` 한 곳에만 둔다. 관리자 API는 DB 오류가 나기 전에 400으로 먼저 막는다(B10).
- **2026-10-06(B4 리뷰) 추가**: `place_photos`에만 `UNIQUE (place_id, position) DEFERRABLE INITIALLY DEFERRED`를 건다(V3). 목록 쿼리가 position 0을 대표 사진으로 읽어서, 같은 position이 둘이면 대표 사진이 하나로 정해지지 않기 때문이다. 순서를 맞바꿀 때 UPDATE 중간에 같은 position이 잠깐 겹치므로 커밋 때 검사한다. 크레딧·출처·임베드는 position을 순서로만 쓰므로 걸지 않는다.
- **2026-10-07(B7 리뷰) 추가**: 저장·팔로우는 PostgreSQL 네이티브 upsert(`INSERT … ON CONFLICT`)로 쓴다. "있으면 갱신, 없으면 저장"을 엔티티로 하면 같은 요청이 동시에 오면 둘 다 "없음"을 보고 INSERT해 중복 키 오류가 난다. `saves`는 상태가 바뀔 때만 `updated_at`을 바꾸고, `follows`는 `DO NOTHING`. 시각은 `Clock` 값을 넘긴다. id를 직접 넣는 다른 엔티티(`DailyPick`)는 `Persistable` 방식을 유지한다.
- **2026-10-07(B8) 추가**: 제보 하루 한도는 `users` 행을 `PESSIMISTIC_WRITE`로 잠근 뒤 센다(Hibernate 7.4.5 PostgreSQL 방언은 `SELECT … FOR NO KEY UPDATE`를 낸다). 세고 나서 넣는 사이에 같은 사용자의 다른 요청이 끼면 둘 다 통과한다(잠금 없이 한도 직전 동시 요청 20개 중 10개 통과를 테스트로 확인). 사용자 한 명의 제보만 줄 세우므로 다른 사용자에게는 영향이 없다.

**채택**: PostgreSQL 18, Flyway, `ddl-auto: validate`, 공개 시 검증.
**포기한 것**: 관리형 DB의 자동 백업(9절에서 직접 백업).

---

## 4. 인증·권한 — Spring Security OAuth2 + 세션

### 선택지

**A. Supabase Auth**: 1절에서 Supabase를 빼면 이것만 따로 쓸 이유가 약하다.

**B. OAuth2 + JWT (프론트가 토큰 보관)**: 서버가 상태를 갖지 않는다. 대신 토큰이 브라우저 JS에 노출되고, 로그아웃·탈퇴·권한 회수를 즉시 반영하기 어렵다.

**C. OAuth2 + 서버 세션(HttpOnly 쿠키)**: 토큰을 JS에 두지 않는다. 로그아웃·탈퇴가 즉시 반영된다. 서버가 하나라 세션 공유 문제가 없다.

### 채택 / 포기한 것

- **채택: C.** 세션은 Spring Session JDBC로 PostgreSQL에 저장해 재배포 후에도 유지한다.
- **제공자(2026-10-06)**: MVP는 **구글만**. 카카오는 개인 개발자 비즈 앱 전환 없이는 이메일을 받지 못하고, 전환 심사가 MVP 일정과 무관한 대기 시간을 만든다. 공개 후 커스텀 provider 하나로 추가한다(SPEC 10절). 관리자도 구글 계정 + `ADMIN_EMAILS`.
- ~~**도메인**: 프론트 `example.kr`, API `api.example.kr`처럼 같은 상위 도메인을 써서 쿠키를 `SameSite=Lax`로 둔다.~~ → 6절 2026-10-06 결정(같은 주소 프록시)으로 대체.
- **역할**: `user`, `admin`. `/api/admin/**`은 admin만(403). 남의 저장·팔로우는 애초에 본인 것만 조회하는 쿼리로 막는다.
- **포기한 것**: 무상태 서버. 서버를 여러 대로 늘릴 계획이 없으므로 대가가 작다.

---

## 5. 콘텐츠 — 저장소 파일 vs DB + 관리자 폼

v0.1은 공간을 저장소의 MDX 파일로 두는 안이었다.

### 선택지

**A. MDX 파일 (v0.1)**

- 장점: 관리자 화면이 필요 없다. 모든 수정이 git에 남는다. 서버가 죽어도 공간 페이지가 보인다.
- 단점: 화면도 API도 DB도 없는 방식이라 풀스택 증거가 남지 않는다. 사용자 제보를 콘텐츠로 이어 붙이기 어렵다.

**B. DB + 관리자 등록 폼**

- 장점: 등록 폼 하나에 CRUD, 양쪽 검증, 파일 업로드, 역할 권한, 상태 흐름, 캐시 갱신, AI 연동이 모두 들어간다. 제보가 같은 테이블의 `suggested` 상태로 자연스럽게 이어진다.
- 단점: 관리자 화면을 만들어야 한다. 콘텐츠 변경 이력이 git에 남지 않는다.

### 채택 / 포기한 것

- **채택: B.** A를 고른 이유였던 Supabase 일시 정지는 DB를 직접 운영하면서 사라졌고, 관리자 화면은 부담이 아니라 보여줄 핵심이 됐다.
- 공개 페이지는 정적으로 만들고, 수정·공개할 때 해당 페이지만 다시 만든다(6절).
- **포기한 것**: git으로 남는 콘텐츠 이력. 필요하면 `updated_at`과 수정자 컬럼으로 최소 이력을 둔다.

---

## 6. 프론트엔드 — Next.js 16 App Router + TypeScript

- React·TS는 현재 프론트엔드의 기본값이다.
- **공개 페이지**(피드, 둘러보기, 공간 상세, 만든 사람 프로필)는 빌드 때 정적으로 만들고, Spring이 공간을 공개·수정하면 Next.js의 **필요할 때 다시 만들기(on-demand revalidation)** API를 호출해 그 페이지만 갱신한다. 호출에는 비밀 토큰을 붙인다.
- 공개 페이지 위의 저장·팔로우 버튼 상태는 로그인 사용자별이라 클라이언트에서 따로 불러온다.
- **로그인 후 화면과 관리자 화면**은 클라이언트 컴포넌트에서 Spring API를 부르고, 서버 상태는 TanStack Query로만 관리한다. 저장·팔로우는 낙관적 업데이트(누르는 즉시 캐시를 바꾸고 서버에 보낸다. 실패하면 이전 값으로 되돌리고 토스트).
- 피드의 세로 넘기기는 CSS scroll-snap으로 만든다. 라이브러리를 쓰지 않는다.
- 스타일은 CSS Modules + CSS 변수. 써 본 방식이라 설명 비용이 없다.
- 관리자 폼은 같은 검증 규칙을 Zod 스키마로 두고, 서버 응답의 필드별 오류도 같은 자리에 보여준다.
- 폼 상태는 라이브러리 없이 `useState`로 든다(2026-10-10, M2a). 값 객체 하나와 `errors`(키 = 서버 `fieldErrors` 키, `senses.touch`·`photos[0].alt`)뿐이고 검증은 Zod가 하므로, react-hook-form이 더해 주는 것은 dirty·필드 배열 정도다. AI 제안을 바깥에서 밀어 넣는 흐름(M5)도 값 객체를 바꾸면 끝난다. 임시저장은 버튼으로만: `PATCH`가 스칼라 전부를 덮어써서 자동 저장이면 공개된 공간에서 타자 중간에 400, slug 입력 중간에 409가 난다.

**채택**: Next.js 16, TypeScript, CSS Modules, TanStack Query, Zod(폼 검증).
**포기한 것**: Tailwind·UI 라이브러리로 얻는 속도, 폼 라이브러리의 dirty·필드 배열 도우미.

**2026-10-06(F1) 결정 변경 — CORS 대신 같은 주소 프록시**: 브라우저는 백엔드를 직접 부르지 않는다. `next.config.ts`의 `rewrites`가 `/api/:path*` · `/oauth2/:path*` · `/login/oauth2/:path*`를 `API_URL`(서버만 아는 주소)로 넘기고, 클라이언트 `apiFetch`는 상대 경로만 쓴다. 이유: 배포에서 프론트와 백엔드 도메인이 다르면 `SameSite=Lax` 세션 쿠키가 교차 출처 fetch에 실리지 않고 `XSRF-TOKEN` 쿠키도 JS가 읽을 수 없다. 같은 출처면 둘 다 그냥 된다. 백엔드의 CORS 설정과 `NEXT_PUBLIC_API_URL`은 없앤다(4절의 "같은 상위 도메인" 요구도 사라진다). `app/api/revalidate/route.ts`는 파일 라우트라 rewrites(배열 = `afterFiles`)보다 먼저 잡힌다([rewrites](https://nextjs.org/docs/app/api-reference/config/next-config-js/rewrites) "static files … and non-dynamic pages are checked/served" 뒤에 `afterFiles`). rewrites 목적지는 `next build` 때 결과물(`routes-manifest.json`·standalone `server.js`)에 굳는다(빌드해서 확인). 그래서 `API_URL`은 실행 환경 변수이면서 빌드 인자이기도 하다(Dockerfile `ARG API_URL`). **포기한 것**: 백엔드 응답이 Next 서버를 한 번 더 거친다(Vercel에서는 함수 호출 하나).

**2026-10-06(F1) 목록 페이지의 렌더 방식**: `/`, `/places`, `/makers`는 segment `revalidate = 0`(요청마다 렌더) + `fetch(next: { tags, revalidate: 3600 })` 데이터 캐시로 간다. 정적 라우트는 빌드 때 prerender되는데 그때 백엔드가 없으면 빌드가 깨지거나 빈 화면이 1시간짜리 HTML로 굳는다(`[slug]` 라우트의 빈 `generateStaticParams` 같은 장치가 정적 라우트에는 없다). 공식 문서([caching-without-cache-components](https://nextjs.org/docs/app/guides/caching-without-cache-components))가 `revalidate = 0`은 "always dynamically rendered … leaves fetch requests that use a positive revalidate as is"라고 적어 이 조합이 보장된다. `force-dynamic`은 문서가 "모든 fetch를 no-store로"라고 적어 쓰지 않는다. 상세 페이지(`[slug]`)는 그대로 빈 `generateStaticParams` + `dynamicParams`(첫 요청 때 생성, 전체 라우트 캐시). 태그 무효화는 두 캐시 모두에 듣는다.

**2026-10-07(F2) 에세이 마크다운 — react-markdown**: 공간 에세이(SPEC 3절 `essay`, 마크다운)는 `react-markdown` 10을 **서버 컴포넌트에서만** 써서 그린다(클라이언트 번들에 들어가지 않는다). `allowedElements`로 h2·h3·p·ul·ol·li·strong·em·a·blockquote만(h1은 h2로 그린다, 이미지 금지), `skipHtml`로 raw HTML을 버리고 HTML 허용 플러그인(`rehype-raw`)은 쓰지 않는다. 링크는 기본 `urlTransform`이 위험한 주소를 비우고 새 창 + `noopener noreferrer`. 확인 결과는 NOTES F2. **포기한 것**: 의존성 없는 직접 파서(제목·문단·목록만이면 짧지만, 링크·인용·강조와 이스케이프를 손으로 맞추는 쪽이 더 위험하다).

**2026-10-06 확인(즉시 반영 방법)**: Spring이 부르는 `app/api/revalidate` 라우트에서는 `revalidateTag(tag, { expire: 0 })`를 쓴다. Next 16은 두 번째 인자가 필수이고, `'max'` 같은 프로필을 주면 다음 요청이 옛 페이지를 먼저 받으므로(stale-while-revalidate) "관리자가 공개한 직후 새로고침 한 번에 보인다"가 되지 않는다. `updateTag`는 Server Action 전용이라 외부 호출 라우트에서 못 쓴다. `cacheComponents`는 켜지 않는다(빈 `generateStaticParams`가 빌드 오류가 됨). 출처는 "버전 기준" 표의 Next.js 행.

**2026-10-07(B6b) 프록시 뒤 OAuth `redirect_uri`**: Next 16.3.8 rewrites 프록시는 Host를 백엔드 주소로 바꾸고 `X-Forwarded-Host`만 덧붙인다(`next/dist/server/lib/router-utils/proxy-request.js`, `X-Forwarded-Proto` 없음). 그래서 구글 `redirect_uri`를 요청에서 만들지 않고 `FRONTEND_URL/login/oauth2/code/google`로 고정한다. Security 7.1.1은 토큰 요청에 처음 저장한 `redirect_uri`를 쓰고 콜백 URL과 비교하지 않아(jar 확인) 고정값으로 충분하다. `server.forward-headers-strategy`는 켜지 않는다. 로컬 8080이 열려 있어 헤더를 믿을 수 없고, Security 문서도 신뢰할 프록시가 헤더를 덮어쓸 때만 쓰라고 한다([Proxy Server Configuration](https://docs.spring.io/spring-security/reference/features/exploits/http.html)). 운영 호스팅이 정해지면(D1) 다시 본다. 로그인 뒤 이동은 상대 경로 `Location`이라 브라우저가 있는 프론트 주소로 간다. Tomcat은 `sendRedirect`의 상대 경로를 Host로 절대 주소화해 `http://backend:8080/…`이 되므로 `server.tomcat.use-relative-redirects: true`를 켠다(curl로 확인, MockMvc에서는 드러나지 않는다). 돌아갈 경로는 ASCII로 퍼센트 인코딩해 보낸다. Tomcat은 ASCII 밖의 `Location`을 내보내지 않아 한글 경로에서 빈 화면이 됐다. fragment(`#` 뒤)는 잘라낸다. 남으면 뒤에 붙이는 `?login=error`가 fragment 안으로 들어갔다.

**2026-10-07(B6b) 구글 토큰은 저장하지 않는다**: `oauth2Login`의 기본 인가 클라이언트 저장소는 받은 액세스 토큰을 프로세스 메모리 맵에 로그인마다 쌓는다. 우리는 구글 API를 부르지 않으므로 아무것도 저장하지 않는 `NoStoreAuthorizedClientRepository`를 넣는다. 쓰지 않는 남의 서비스 토큰은 서버가 뚫렸을 때 노출될 정보만 늘린다. 구글 API가 필요해지면 그때 저장소를 정한다.

**2026-10-10(B13) 재생성 호출**: 백엔드가 프론트를 부르는 주소(`REVALIDATE_URL`)를 공개 주소(`FRONTEND_URL`)와 나눈다. `FRONTEND_URL`은 구글 `redirect_uri`라 브라우저가 보는 주소여야 하는데, compose 안의 백엔드 컨테이너에서 `localhost:3000`은 자기 자신이다. 비우면 `FRONTEND_URL`을 쓴다. 호출은 커밋 직후 관리자 요청 스레드에서 한 번만 보낸다(연결 2초·읽기 5초). 재시도·큐·`@Async`를 두지 않는 이유는 1시간 주기 재생성이 이미 안전망이고(8절), 재시도는 관리자 응답만 늦추기 때문이다. 커밋 뒤 HTTP 호출 동안 DB 연결도 붙잡는다(최대 7초, 관리자 경로라 허용). JDK `HttpClient`는 HTTP/1.1로 고정한다. 기본값은 `http://`에도 h2c 업그레이드 헤더를 붙이는데, Next 서버가 그 요청을 응답 없이 끊었다(compose에서 curl `--http2`로 재현). 공휴일은 다루지 않는다. 편성이 평일만 따지고, 공휴일에 칸을 비워도 08:00에는 피드의 다음 공개 시각이 바뀌므로 재생성이 필요하다.

**2026-10-10(M3) 관리자 화면의 작업용 숫자**: "숫자 표시 금지"(좋아요·저장 수·팔로워 수·조회수·랭킹)는 공개 화면과 공개 API가 대상이다. 관리자 화면의 작업용 숫자(만든 사람 목록·찾기 칸의 "공간 n곳", API의 `placeCount`)는 예외로 둔다. 인기나 순위가 아니라 크레딧을 정리할 때 같은 사람을 고르게 돕는 값이고, 관리자만 본다.

---

## 7. 사진 — Cloudflare R2 + 서명된 URL 직접 업로드

### 선택지

**A. 서버가 파일을 받아 저장소로 전달**: 구현이 단순하다. 대신 서버 메모리·대역폭을 쓰고, 큰 파일에서 느리다.

**B. 서버는 서명된 URL만 발급, 브라우저가 저장소에 직접 업로드**: 서버를 파일이 거치지 않는다. 대신 업로드 후 등록이 빠진 고아 파일을 정리해야 한다.

### 채택 / 포기한 것

- **채택: B.** 서명된 URL은 5분짜리, 관리자만 발급. 등록되지 않은 채 24시간 지난 파일은 매일 정리 작업이 지운다.
- 저장소는 **R2**: 10GB까지 무료이고 내보내기 트래픽이 무료다. **S3 호환 API**라서 AWS SDK로 그대로 쓴다.
- 변환은 **브라우저에서** WebP 두 크기(1600px, 800px)로 한다. 서버에 이미지 처리 라이브러리를 두지 않고, Vercel 무료 플랜의 이미지 변환 한도(월 5,000회)도 쓰지 않기 위해서다.
- **포기한 것**: 서버 쪽 이미지 처리의 일관성(브라우저마다 인코딩 품질이 조금 다를 수 있다).
- **2026-10-06 추가(사진 URL 전달, PLAN 확인 필요 14)**: Vercel Hobby의 월 5,000회 변환 한도는 `remotePatterns`로 받는 원격 이미지에도 적용된다. 이미 두 크기로 올라간 WebP를 다시 변환할 이유가 없으므로 서버가 `PHOTO_PUBLIC_BASE_URL + objectKey`로 완성한 URL(`w1600`·`w800`)을 주고 프론트는 `<img srcset>`으로 그린다. Next 이미지 최적화는 쓰지 않는다. 운영 공개 도메인은 R2 커스텀 도메인(r2.dev는 개발 전용).
- **저장소(2026-10-06)**: 개발·운영 모두 R2, 버킷만 다르다(12-2절). compose에 저장소 컨테이너가 없다.
- **사진 출처(2026-10-06)**: 사용 허락을 받는 절차는 두지 않는다. `license`는 `own`·`press`·`credited`, `own`이 아니면 `source_url` 필수, 화면에 "사진: ○○"과 링크. 공식 인스타그램 공개 게시물은 임베드 우선. 삭제 요청 48시간.

---

## 8. 예약 공개 — 하루 한 곳 피드

### 선택지

**A. 스케줄러가 매일 08:00에 "공개" 플래그를 켠다**: 직관적이다. 대신 스케줄러가 한 번 실패하면 그날 공간이 열리지 않는다.

**B. 공개 여부를 조회 시점에 계산한다** (`pick_date 08:00 KST <= 지금`): 스케줄러가 없어도 정확하다. 홈 페이지가 정적이라 캐시 갱신은 따로 필요하다.

### 채택 / 포기한 것

- **채택: B + 캐시 갱신용 스케줄러.** 피드 API는 조회 시점에 계산한다. Spring `@Scheduled`가 매일 08:00(KST)에 홈 재생성을 호출하고, 실패에 대비해 홈은 1시간마다도 다시 만든다. 공개 시각이 "데이터"로 결정되므로 테스트에서 시계를 고정해 검증할 수 있다.
- 시간은 DB와 서버 모두 UTC로 저장하고, 08:00 KST 경계 계산만 `Asia/Seoul`로 한다.
- **포기한 것**: 스케줄러 하나로 끝나는 단순함.

---

## 9. 배포 — 보류

호스팅과 계정 문제는 보류했다. 확정된 것만 적는다.

- 프론트: Vercel Hobby(비상업). 수익화하면 Pro.
- 백엔드, PostgreSQL, AI 초안 서비스는 **Docker Compose로 서버 한 대**에 올린다. 로컬의 `docker compose up` 구성을 그대로 운영으로 가져간다.
- HTTPS는 리버스 프록시의 자동 인증서. DB는 매일 `pg_dump` → R2로 백업하고, 복원을 한 번 실제로 해 본다.
- 서버 후보: AWS EC2(가장 널리 쓰임, 무료 기간 이후 월 2~3만 원 추정), AWS Lightsail(정액), 무료 PaaS + 무료 Postgres. **결정 마감: PLAN M4(AI 국면 끝, 10/13 — 2026-10-07 체크포인트로 10/20에서 앞당김).** D1이 이 결정에 의존한다.

---

## 10. AI 초안 서비스

관리자 폼의 "AI로 초안 채우기"(SPEC 6절) 뒤에서 동작한다. **순서(2026-10-06)**: 관리자 폼이 완성된 뒤에 만든다(PLAN 국면 A). 데이터 모양(`AdminPlace`, `PublishRules`)이 확정된 상태로 붙이는 쪽이 단순하고, AI가 먼저 올 때 필요했던 임시 장치(파일 호출 기록)가 없어진다.

### 언어: TypeScript vs Python

- TS면 언어가 셋에서 둘로 줄어든다.
- 그러나 LLM 앱 생태계의 기본 스택이 Python이다(FastAPI, LangChain·LangGraph, 각 제공자 SDK). 구조화 출력도 Pydantic이 표준에 가깝다.
- **채택: Python 3.14 + FastAPI.** Spring이 이 서비스를 내부 네트워크로 호출한다(외부에 열지 않음). 독립된 작은 서비스라 언어가 하나 늘어나는 부담을 견딜 만하다.

### 구조 (단계적으로)

1. **프레임워크 없이** LLM SDK로 Tool Calling 루프를 직접 구현한다. 도구: 웹 검색(Tavily), 페이지 읽기, 지오코딩(카카오 로컬), 결과 검증.
2. 출력은 Pydantic 모델로 받고, 검증에 실패하면 오류를 모델에 돌려 재시도한다. **필드마다 근거 URL**을 받고, 근거가 없으면 비운다. 감상·감각·한 줄은 생성하지 않는다.
3. 같은 흐름을 **LangGraph로 옮겨** 두 구현을 비교한 회고를 남긴다(공개 후).
4. 같은 도구를 **MCP 서버**로 감싸 Claude Desktop에서 호출하는 데모를 남긴다.

### LLM 제공자

- 제공자를 하나로 못 박지 않는다. 호출을 얇은 인터페이스 하나 뒤에 둔다. 기본 제공자는 구현 시작 때 정하고, 평가셋으로 다른 하나와 한 번 비교한다.

### 평가와 미터링

- **평가셋**: 크레딧이 확인된 공간 20곳을 정답으로 둔다. 필드별 정확도, 근거 URL 일치율, 지어낸 값 비율, 건당 비용·시간을 같은 표로 측정한다. 프롬프트나 도구를 바꿀 때마다 표를 다시 뽑아 README에 남긴다.
- **미터링**: 호출마다 `llm_calls`에 모델, 토큰, 비용, 지연, 재시도, 성공 여부를 쓴다. 관리자 화면 `/admin/llm`에서 본다.
- 평가는 비용이 드니 CI에서 자동으로 돌리지 않고 수동 실행 워크플로로 둔다.

**채택**: Python + FastAPI, 직접 구현 → LangGraph 이식, MCP 서버, 평가셋 20개, `llm_calls`.
**포기한 것**: 언어 수 최소화. 멀티 에이전트·파인튜닝(이 프로젝트의 범위 밖).

---

## 11. CI · 테스트 · 협업

- **GitHub Actions**: PR마다 ① 프론트 타입 검사·lint·Vitest ② 백엔드 `./gradlew test`(Testcontainers) ③ AI 서비스 pytest(외부 호출은 가짜로 대체). 하나라도 실패하면 병합 불가.
- **백엔드 통합 테스트(최소)**: 공개 시 검증(필수 누락 400) · 상태 전환 규칙(published → draft 불가) · slug 공개 후 변경 불가 · 관리자 API 일반 사용자 403 · 저장 상태 전환과 해제 · 팔로우한 사람의 공간 조회(중복 제거, `following_seen_at` 기준 "새로운" 표시와 탭 점) · 피드 공개 경계(07:59 / 08:00, 월요일 8시 전 지난주) · 목록 API 쿼리 수 · 탈퇴 시 데이터 처리.
- **프론트**: 순수 함수(피드 마지막 화면 문구, 요일 계산, 폼 검증 스키마)만 Vitest.
- **협업 흔적**: 혼자여도 기능 단위 브랜치 → PR → 셀프 리뷰 코멘트 → 병합. PR 템플릿에 "무엇을, 왜, 어떻게 확인했나".
- **AI 코딩도구**: Claude Code를 쓰고, CLAUDE.md와 PLAN.md 운영 방식을 README에 공개한다.

---

## 12. 2026-10-06 검수에서 드러난 두 가지 — 채택 (같은 날 결정)

PLAN 검수 중 공식 문서로 확인하다가 v0.1의 전제 두 개가 깨졌다. 후보를 비교하고 같은 날 대화로 결정했다.

### 12-1. AI 초안 서비스의 웹 검색 도구 (네이버 검색 API 대체)

네이버 검색 API는 신규 신청이 끝났고(2026-07-30, NAVER API HUB로 이전) 약관이 "데이터를 AI에 입력" 자체를 금지한다. A3의 루프는 검색 결과를 모델에 넣는 것이 전부라 쓸 수 없다. 공통 전제: **검색 결과는 저장하지 않고 그 호출 안에서만 쓴다. DB에는 모델이 고른 근거 URL만 남는다**(`llm_calls`에는 토큰·비용만). 이 전제면 아래 셋은 약관 안이다.

| 후보 | 무료 | 약관(AI 입력) | 한국어 | 출처 |
| --- | --- | --- | --- | --- |
| **Tavily** | 월 1,000 크레딧(기본 검색 1크레딧), 카드 없음 | LLM 입력이 제품 목적. 금지는 "경쟁 모델 개발"만 | 명시 없음 | [크레딧](https://docs.tavily.com/documentation/api-credits) · [약관](https://tavily.com/terms) |
| **Brave Search API** | 월 $5 크레딧(≈1,000건), 카드 필요 | 일시적 사용은 허용, **저장·캐싱·학습 금지**(저장은 별도 플랜) | `country=KR`, `search_lang=ko` 지원 | [소개](https://brave.com/search/api/) · [약관](https://api-dashboard.search.brave.com/terms-of-service) |
| **카카오 Daum 검색**(웹문서·블로그·카페) | 전체 5만 건/일, API별 3만 건/일. 로그인·로컬과 같은 앱 | AI 조항 없음. 제11조 3항 "복제·변경·검색엔진 입력" 금지 — 일시적 컨텍스트 사용은 회색 | 한국 블로그·카페에 강함(공식 언급은 없음) | [가이드](https://developers.kakao.com/docs/latest/ko/daum-search/dev-guide) · [쿼터](https://developers.kakao.com/docs/latest/ko/getting-started/quota) · [약관](https://developers.kakao.com/terms/latest/ko/site-terms) |
| NAVER API HUB(NCP) | 한시 무료, 종량제 예정 | **2026-10-07 시행 약관 개정 공지가 있는데 본문을 못 읽었다.** developers.naver.com 약관을 그대로 옮겼을 가능성이 크다 | 네이버 | [개요](https://guide.ncloud-docs.com/docs/apihub-overview) · [공지](https://www.ncloud.com/support/notice/all/2271) |
| Google Custom Search JSON | 100건/일 | 신규 고객 마감, 2027-01-01 종료 | — | [개요](https://developers.google.com/custom-search/v1/overview) |

**채택: Tavily를 기본 `web_search`로.** 무료 한도가 평가셋 20곳 × 호출 수십 번에 충분하고, 카드가 필요 없고, 약관이 가장 명확하다. 카카오 Daum 검색은 PLAN A5의 비교 실험용으로만 쓴다. **원칙: 검색 결과는 저장하지 않고 그 호출 안에서만 쓴다.** DB에는 모델이 고른 근거 URL과 `llm_calls`(토큰·비용)만 남는다. 어느 검색 API의 약관에도 걸리지 않는 선이다.
**포기한 것**: 네이버의 국내 검색 품질. 지표가 나쁘면 A5에서 카카오 Daum 검색을 보조 도구로 올린다.

### 12-2. 로컬 S3 호환 저장소 (MinIO 대체)

MinIO 커뮤니티 에디션은 저장소가 보관(archived) 처리됐고 Docker Hub 이미지가 사라졌다(버전 기준 표). 코드는 어느 쪽이든 S3 API만 쓰고 테스트는 컨테이너 없이 하므로(PLAN B11), 영향은 compose 한 서비스와 `.env.example` 값이다.

| 후보 | 장점 | 단점 |
| --- | --- | --- |
| **R2에 개발용 버킷을 따로 두고 로컬도 R2를 쓴다** | 새 의존성 0. 운영과 같은 서명·CORS 동작을 개발 때부터 본다(`chunkedEncodingEnabled(false)` 같은 R2 특이점이 D2가 아니라 B11에서 드러남). 무료 한도 안 | compose가 완전히 오프라인이 아니다. Cloudflare 계정·버킷을 S1에 만들어야 한다. 공개 읽기는 r2.dev(개발 전용 허용) |
| 다른 S3 호환 서버 컨테이너(RustFS, Garage, SeaweedFS, LocalStack 등) | 오프라인 | 어느 것도 공식 문서로 확인하지 않았다. 새 의존성 하나 + 버킷 생성 1회 작업 + 공개 읽기 설정을 또 익혀야 한다 |
| 로컬 디스크에 쓰는 가짜 `ObjectStorage` 구현 | 가장 단순 | 서명 URL·브라우저 직접 업로드(M4)를 로컬에서 못 본다. 포기 |

**채택: 첫 번째(R2 개발용 버킷).** 이 프로젝트에서 로컬 저장소의 역할은 "R2 없이도 돌아간다"가 아니라 "업로드 흐름을 확인한다"였고, 그 확인은 진짜 R2에서 하는 게 더 정확하다. compose에서 저장소 서비스를 빼고 `S3_*` 환경 변수가 개발 버킷을 가리킨다. 테스트는 컨테이너 없이(서명은 오프라인, 고아 선별은 가짜 구현).
**포기한 것**: 완전히 오프라인인 개발 환경.

---

## 확인이 필요한 결정

1. **호스팅과 계정** (9절, 보류).
2. **Spring Boot vs NestJS**: Spring을 권한다. 생태계 크기와 써 본 경험 때문이다.
3. **JPA vs JdbcTemplate**: JPA를 권한다. 두 방식을 비교해 말할 수 있게 된다.
4. **AI 서비스를 Python으로**: LLM 앱 생태계가 Python 중심이라 권한다. 언어가 셋이 되는 부담이 있다.
5. **LLM 기본 제공자**: 구현 시작 때 정한다(PLAN 확인 필요 8: 가정은 Anthropic).
6. ~~웹 검색 도구~~ → **Tavily 채택** (12-1절, 2026-10-06).
7. ~~로컬 S3 호환 저장소~~ → **R2 개발용 버킷 채택** (12-2절, 2026-10-06).
8. ~~로그인 제공자~~ → **MVP는 구글만** (4절, 2026-10-06).

## 참고

- [Next.js 16 공식 발표](https://nextjs.org/blog/next-16)
- [Spring Boot 4: What Actually Changed](https://katyella.com/blog/spring-boot-4-should-you-upgrade/)
- [AWS Free Tier FAQ](https://aws.amazon.com/free/free-tier-faqs/)
- [Supabase 무료 요금제 한도](https://www.itpathsolutions.com/supabase-free-tier-limits)
- [Cloudflare R2 무료 요금제](https://nubbo.app/blog/cloudflare-r2-free-tier/)
- [Vercel Hobby 제약과 한도](https://justinmckelvey.com/blog/is-vercel-free)
