# 운영 절차

운영 서버를 다루는 방법. 값(비밀번호·키·IP)은 여기 적지 않는다. 결정의 이유는 `docs/DECISIONS.md` 9절.

## 1. 구성

| 주소 | 어디 | 무엇 |
| --- | --- | --- |
| `jieuni.space` | Vercel(함수 리전 `icn1`) | 프론트. `/api/**`·`/oauth2/**`·`/login/oauth2/**`를 `api.jieuni.space`로 넘긴다. `www`는 여기로 308 |
| `api.jieuni.space` | EC2 한 대(서울, t4g.small) | `~/jieuni/deploy`의 compose: `caddy`(80·443, 인증서) → `backend`(8080) → `db`(5432) |
| `photos.jieuni.space` | R2 운영 버킷 `jieuni` | 사진 공개 읽기. r2.dev 주소는 꺼 둔다 |

- 8080·5432는 호스트에 열지 않는다. 밖에서는 Caddy의 80·443만 보인다.
- 백엔드 이미지는 `.github/workflows/release.yml`이 만든다: `ghcr.io/smosco/jieuni-backend:<짧은 sha>`, `main`이면 `latest`도. 서버는 빌드하지 않고 받기만 한다.
- 프론트는 Vercel이 `main` push마다 빌드해 운영에 배포한다. 다른 브랜치는 프리뷰.
- 백엔드 비밀값은 서버의 `deploy/.env`, 프론트 비밀값은 Vercel 환경 변수에만 있다. 이름은 `deploy/.env.example`과 아래 Vercel 표.

## 2. 날짜

| 항목 | 날짜 |
| --- | --- |
| AWS 계정 생성일 | ____ |
| Free plan 만료(생성일 + 6개월, 또는 크레딧 소진) | ____ |
| Paid plan 전환 예정일(만료 2주 전) | ____ |

Free plan이 끝나면 계정이 자동으로 닫히고 리소스에 접근할 수 없다. AWS는 90일 동안 보관한 뒤 지운다. 그 안에 Paid plan으로 올리면 되살아나지만 그동안 서비스는 멈춘다. 그래서 만료 전에 올린다: 콘솔 → [Upgrade plan](https://console.aws.amazon.com/billing/home?#/freetier/upgrade) → Upgrade account. 남은 크레딧은 Billing and Cost Management 콘솔 홈에서 본다. ([Choosing a plan](https://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/free-tier-plans.html))

## 3. 처음 한 번

순서가 중요하다. DNS가 EIP를 가리키기 전에 Caddy를 띄우면 인증서 검증이 실패하고 Let's Encrypt 한도를 쓴다.

**GitHub**

1. Release는 `main`에 `backend/**`가 머지되면 자동으로 돈다. Actions → Release → Run workflow 버튼은 워크플로 파일이 기본 브랜치(`main`)에 있어야 보이므로, 머지 뒤 같은 커밋을 다시 돌릴 때만 쓴다. 끝나면 Packages에 `jieuni-backend`가 생긴다. 태그(짧은 sha)를 적어 둔다.
2. 패키지 `jieuni-backend` → Package settings → Danger Zone → Change visibility → Public. 되돌릴 수 없다. 이후 서버는 로그인 없이 받는다.

**AWS 콘솔**

3. 오른쪽 위 리전을 **아시아 태평양(서울) `ap-northeast-2`**로.
4. EC2 → Launch instance.
   - AMI: **Ubuntu Server 24.04 LTS**, 아키텍처 **64-bit (Arm)**.
   - Instance type: **t4g.small**. 목록에 "Free tier eligible" 표시가 있는지 본다. 2025-07-15 이후 계정은 `t3.micro`·`t3.small`·`t4g.micro`·`t4g.small`·`c7i-flex.large`·`m7i-flex.large`가 대상이다([EC2 Free Tier](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-free-tier-usage.html)). 표시가 헷갈리면 콘솔 위쪽 CloudShell에서:
     ```
     aws ec2 describe-instance-types --region ap-northeast-2 \
       --filters Name=free-tier-eligible,Values=true \
       --query "InstanceTypes[*].[InstanceType]" --output text | sort
     ```
   - Key pair: 새로 만들기(ED25519, `.pem`). 받은 파일은 `chmod 400`.
   - Network settings → 보안 그룹 새로 만들기: SSH(22) **My IP**, HTTP(80) Anywhere, HTTPS(443) Anywhere.
   - Storage: 20 GiB gp3.
5. EC2 → Elastic IPs → Allocate → 만든 주소 선택 → Associate → 인스턴스. 공인 IPv4는 시간당 요금이 붙어 크레딧에서 빠진다.

**서버(SSH)**

6. 접속: `ssh -i <키>.pem ubuntu@<EIP>`
7. 스왑 2GB(JVM + Postgres를 2GB에 올리므로):
   ```
   sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
   sudo mkswap /swapfile && sudo swapon /swapfile
   echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
   ```
8. Docker Engine + compose 플러그인: [Install Docker Engine on Ubuntu](https://docs.docker.com/engine/install/ubuntu/)의 "Install using the apt repository" 절차대로(`docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin`). 이어서 `sudo usermod -aG docker ubuntu` 후 다시 접속.
9. 로컬에서 구성 파일 복사:
   ```
   ssh -i <키>.pem ubuntu@<EIP> mkdir -p jieuni
   scp -i <키>.pem -r deploy ubuntu@<EIP>:~/jieuni/
   ```
10. 서버에서 `.env`:
    ```
    cd ~/jieuni/deploy && cp .env.example .env && chmod 600 .env
    ```
    `BACKEND_TAG`에 1단계의 태그를 넣는다(`main`에 병합되어 `latest`가 생기기 전까지). 나머지 값은 아래처럼 채운다. 비어 있으면 백엔드 시작 검사가 멈추고 빠진 이름을 모두 로그에 남긴다.

    - **새로 만드는 운영 비밀값**: `POSTGRES_PASSWORD`, `REVALIDATE_SECRET`. 개발 값을 쓰지 않고 각각 새로 만든다. `REVALIDATE_SECRET`은 27단계에서 Vercel 환경 변수에 같은 값을 넣는다.
      ```
      openssl rand -hex 32
      ```
    - **임시값(23단계 전까지)**: 이 시점에는 운영 R2 토큰, 사진 도메인, 구글 운영 클라이언트가 아직 없다. 아래 값을 넣어 두고 23단계에서 실제 값으로 바꾼다. 그동안 사진 업로드와 구글 로그인은 동작하지 않는다.
      ```
      S3_ENDPOINT=https://pending.invalid
      PHOTO_PUBLIC_BASE_URL=https://pending.invalid
      S3_BUCKET=pending
      S3_ACCESS_KEY=pending
      S3_SECRET_KEY=pending
      GOOGLE_CLIENT_ID=pending
      GOOGLE_CLIENT_SECRET=pending
      ```
      `S3_ENDPOINT`와 `PHOTO_PUBLIC_BASE_URL`은 `https://` 주소 모양이어야 한다. `S3_ENDPOINT=pending`처럼 scheme이 없으면 S3 클라이언트를 만들 때 `The URI scheme of endpointOverride must not be null`로 시작이 멈춘다. `.invalid`는 실제로 해석되지 않는 예약 도메인이다.
    - **그대로 넣는 값**: `FRONTEND_URL=https://jieuni.space`. `REVALIDATE_URL`은 비운다. `ADMIN_EMAILS`에는 관리자 구글 이메일을 넣는다.

    다 채운 `.env` 전체를 비밀번호 관리자 등 서버 밖 한 곳에 보관하고, 값을 바꿀 때마다 갱신한다. 서버를 잃으면 이 값들도 함께 사라진다(12절).

**Cloudflare**

11. 존 `jieuni.space` → DNS → Records → Add record: Type `A`, Name `api`, IPv4 `<EIP>`, Proxy status **끔(DNS only, 회색 구름)**.
12. 로컬에서 `dig +short api.jieuni.space`가 EIP를 돌려줄 때까지 기다린다.

**시작**

13. `cd ~/jieuni/deploy && docker compose up -d`, 이어서 `docker compose logs -f caddy`에서 인증서 발급(`certificate obtained successfully`)을 본다.
14. 9절 확인 명령.

여기부터는 프론트·사진·로그인이다. R2와 구글 값을 먼저 만들어 서버 `.env`를 한 번에 바꾸고, 도메인은 Vercel 빌드가 통과한 뒤에 붙인다. 화면에 한 번만 나오는 키는 바로 서버 `.env`나 Vercel에 옮기고 다른 곳(채팅·메모·저장소)에 적지 않는다.

**R2(Cloudflare → R2 Object Storage)**

15. **Create bucket**: 이름 `jieuni`, Location Automatic(위치 힌트 Asia-Pacific), Default storage class Standard.
16. 버킷 → **Settings → Custom Domains → Add** → `photos.jieuni.space` → Continue → **Connect Domain**. Cloudflare가 DNS 레코드를 직접 만든다. 이 레코드는 고치지 않는다(R2 커스텀 도메인은 Cloudflare 프록시를 거치므로 이것만 주황 구름이 정상). 상태가 Active가 될 때까지 기다린다([공개 버킷](https://developers.cloudflare.com/r2/buckets/public-buckets/)).
17. 같은 화면 **Public Development URL → Disable** → `disallow` 입력 → **Disallow**.
18. **Settings → CORS Policy → Add CORS policy**:
    ```json
    [{"AllowedOrigins":["https://jieuni.space"],"AllowedMethods":["PUT"],"AllowedHeaders":["Content-Type"],"ExposeHeaders":["ETag"],"MaxAgeSeconds":3600}]
    ```
    서명 URL은 `Content-Type`만 서명하므로 허용 헤더는 이것 하나다.
19. R2 → **Manage API tokens → Create API token**: 이름 `jieuni-prod`, 권한 **Object Read & Write**, **Apply to specific buckets only → `jieuni`**, TTL Forever. 나오는 Access Key ID · Secret Access Key와 S3 endpoint(`https://<계정ID>.r2.cloudflarestorage.com`)를 23단계에 쓴다.

**구글(Google Cloud 콘솔 → Google Auth Platform)**

운영 클라이언트는 개발 클라이언트와 따로 만든다(DECISIONS 9절).

20. **Branding** 먼저: 앱 이름 `공간의 지은이`, 지원 이메일, **Authorized domains**에 `jieuni.space`. 로고는 올리지 않는다(올리면 브랜드 검증 대상이 된다).
21. **Clients → Create client** → Application type **Web application**, 이름 `jieuni-prod`. **Authorized redirect URIs**에 `https://jieuni.space/login/oauth2/code/google` 하나만. Authorized JavaScript origins는 비운다(서버 쪽 흐름). Client ID · Client secret을 23단계에 쓴다.
22. **Audience**: Publishing status가 Testing이면 테스트 사용자만 로그인된다 → **Publish app**. 범위가 `openid email profile`뿐이라 검토 없이 In production이 되어야 한다.
    - Publish가 홈페이지·개인정보처리방침 URL을 요구하면: **Testing을 유지**하고 **Test users → Add users**에 관리자 계정을 넣어 운영 확인(11절)을 먼저 끝낸다. `/privacy`가 생긴 뒤(D4) Branding에 홈페이지 `https://jieuni.space`·개인정보처리방침 `https://jieuni.space/privacy`를 넣고 Publish한다. 그 전에는 다른 사람이 로그인하지 못한다.

**서버 `.env` 실제 값**

23. 서버에서 백업부터:
    ```
    cd ~/jieuni/deploy && cp .env .env.bak && chmod 600 .env.bak
    ```
    `.env`에서 바꿀 값:
    - `S3_ENDPOINT`(19단계 endpoint), `S3_REGION=auto`, `S3_BUCKET=jieuni`, `S3_ACCESS_KEY`·`S3_SECRET_KEY`(19단계 토큰)
    - `PHOTO_PUBLIC_BASE_URL=https://photos.jieuni.space`
    - `GOOGLE_CLIENT_ID`·`GOOGLE_CLIENT_SECRET`(21단계)
    - `FRONTEND_URL`·`REVALIDATE_SECRET`·`ADMIN_EMAILS`·DB 값·`BACKEND_TAG`는 그대로.
24. `docker compose up -d backend`, `docker compose logs -f backend`에 `prod 필수 환경 변수` 오류가 없고 9절 health가 200인지 본다. 문제가 생기면 `cp .env.bak .env` 후 다시 `up -d backend`.

**Vercel**

25. **Add New → Project → Import** 저장소 `smosco/jieuni`. Framework Next.js(자동), **Root Directory `frontend`**. Node 버전은 `frontend/package.json`의 `engines.node`(24.x)를 Vercel이 읽으므로 고르지 않는다([Node.js 버전](https://vercel.com/docs/functions/runtimes/node-js/node-js-versions)). 같은 화면 **Environment Variables**에 27단계 값을 넣고 Deploy. 처음부터 넣어야 첫 빌드가 멈추지 않는다.
26. **Settings → Functions → Function Regions**에서 **Seoul (`icn1`)** 하나만(기본은 `iad1`, Hobby는 한 리전). 저장 후 **Deployments → 최신 배포 → Redeploy**.
27. **Settings → Environment Variables**. 모두 빌드 때 굳으므로 값을 바꾸면 Redeploy한다.

    | 이름 | 값 / 출처 | 환경 |
    | --- | --- | --- |
    | `API_URL` | `https://api.jieuni.space` | Production, Preview |
    | `PHOTO_PUBLIC_BASE_URL` | `https://photos.jieuni.space`(16단계). CSP `img-src`. 없으면 운영 빌드가 멈춘다 | Production, Preview |
    | `NEXT_PUBLIC_SITE_URL` | `https://jieuni.space`. canonical·공유 카드·sitemap의 바탕 | Production |
    | `REVALIDATE_SECRET` | 서버 `.env`와 같은 값. **Sensitive** 체크 | Production |
    | `NEXT_PUBLIC_NEWSLETTER_URL` | 메일리 폼 주소. 비우면 피드 끝 구독 링크가 안 보인다 | Production |

    `REVALIDATE_SECRET`은 서버에서 자기 터미널로만 꺼내 붙인다: `grep '^REVALIDATE_SECRET=' ~/jieuni/deploy/.env | cut -d= -f2-`.

    **프리뷰**: 프리뷰에는 `API_URL`·`PHOTO_PUBLIC_BASE_URL`만 둔다(`API_URL`이 없으면 빌드가 멈춘다). 프리뷰는 운영 백엔드의 공개 데이터를 읽고, 프리뷰에서 구글 로그인하면 운영(`FRONTEND_URL`)으로 돌아간다. canonical이 `localhost`로 나오지만 Vercel이 프리뷰에 `X-Robots-Tag: noindex`를 붙인다.

**도메인(Vercel + Cloudflare)**

28. Vercel **Settings → Domains → Add Domain** → `jieuni.space`. `www`를 함께 추가하라는 제안을 받고 **`www.jieuni.space` → `jieuni.space`(308)** 리디렉트로 둔다. apex가 정식 주소다(`FRONTEND_URL`·`NEXT_PUBLIC_SITE_URL`과 같게).
29. Cloudflare → 존 `jieuni.space` → **DNS → Records → Add record**, 값은 Vercel 도메인 카드에 나온 그대로:
    - Type `A`, Name `@`, IPv4 = 카드의 값, Proxy status **DNS only(회색 구름)**
    - Type `CNAME`, Name `www`, Target = 카드의 값(프로젝트마다 다르다), **DNS only**
    
    같은 이름의 다른 A·AAAA·CNAME은 지운다. CAA 레코드가 있으면 `letsencrypt.org`를 허용하는지 본다(Vercel 인증서).
30. Vercel Domains에서 두 도메인이 Valid Configuration이 되고 인증서가 나올 때까지 기다린다.

**머지 뒤**

31. 작업 브랜치를 `main`에 머지한다. `backend/**`가 바뀌었으면 Release가 `latest`를 만들고(1단계), Vercel은 운영으로 다시 배포한다.
32. 서버 `.env`에서 `BACKEND_TAG`를 비운다(→ `latest`). `REVALIDATE_URL`에 값을 넣어 두었다면 그것도 비운다(비우면 `FRONTEND_URL`로 부른다). 그다음 4절대로 받고 띄운다.
33. 11절 운영 확인.

## 4. 새 이미지 배포

`main`에 `backend/**`가 병합되면 Release가 `latest`를 새로 만든다. 서버에서:

```
cd ~/jieuni/deploy
docker compose pull backend && docker compose up -d backend
docker image prune -af
```

`prune -af`는 돌고 있지 않은 이미지를 모두 지운다. 되돌릴 때는 GHCR에서 다시 받으면 된다(7절).

프론트는 따로 할 일이 없다. `main` push마다 Vercel이 빌드해 운영에 배포하고, 진행은 Vercel → Deployments에서 본다.

## 5. 로그

```
docker compose logs -f --tail 200 backend   # caddy, db도 같은 방식
```

로그는 컨테이너마다 10MB × 3개까지만 남는다.

## 6. 재시작

```
docker compose restart backend      # 백엔드만
docker compose down && docker compose up -d   # 전체
```

`down -v`는 쓰지 않는다. DB(`db-data`)와 인증서(`caddy-data`)가 지워진다.

`Caddyfile`을 고쳤으면 `docker compose restart caddy`.

## 7. 되돌리기

1. GitHub → Packages → `jieuni-backend`에서 되돌릴 태그(짧은 sha)를 고른다.
2. `.env`의 `BACKEND_TAG=<sha>`로 바꾸고 `docker compose up -d backend`. 지운 이미지는 이때 GHCR에서 다시 받는다.
3. 고친 커밋이 `main`에 들어가 새 `latest`가 생기면 `BACKEND_TAG`를 비우고 4절대로 배포한다.

프론트는 Vercel → Deployments → 되돌릴 배포의 ⋯ → **Instant Rollback**. 그 뒤 `main` push가 와도 자동 배포가 멈춰 있으므로, 고친 커밋이 들어가면 그 배포를 **Promote**해 되살린다.

**마이그레이션과 되돌리기**: Flyway 기본 `ignoreMigrationPatterns`가 `*:future`라 DB가 이전 이미지보다 앞선 마이그레이션을 갖고 있어도 이전 이미지는 멈추지 않고 뜬다. 위험은 파괴적 마이그레이션(열 삭제·이름 변경) 뒤에 되돌리는 경우다. 이전 코드가 없어진 열을 찾으면서 쿼리가 깨진다. 엔티티가 매핑한 열이면 `ddl-auto: validate`에서 시작부터 멈춘다. 그래서 열 삭제·이름 변경은 "새 열 추가 → 코드 전환 배포 → 다음 배포에서 옛 열 삭제" 두 단계로 나눈다.

## 8. 환경 변수 바꾸기

`.env`를 고친 뒤 `docker compose up -d backend`. compose가 바뀐 값을 보고 컨테이너를 다시 만든다. `restart`는 새 값을 읽지 않는다.

Vercel 환경 변수는 빌드 때 굳는다. 바꾼 뒤 Deployments → 최신 배포 → **Redeploy**.

바꾼 뒤 서버 밖에 보관한 `.env` 사본도 같이 갱신한다(3절 10단계).

## 9. 확인 명령

로컬에서:

```
curl -sI https://api.jieuni.space/actuator/health
# HTTP/2 200, set-cookie: XSRF-TOKEN=…; Path=/; Secure

curl -vI https://api.jieuni.space 2>&1 | grep -i issuer
# issuer: … Let's Encrypt

nc -z -G 3 <EIP> 8080; nc -z -G 3 <EIP> 5432
# 둘 다 실패해야 한다(외부에 닫힘)
```

개발용 로그인이 없는지(CSRF 토큰 없이 보내면 CSRF가 먼저 막아 403이 나온다):

```
curl -s -c /tmp/j -o /dev/null https://api.jieuni.space/actuator/health
T=$(awk '/XSRF-TOKEN/{print $7}' /tmp/j)
curl -s -b /tmp/j -H "X-XSRF-TOKEN: $T" -o /dev/null -w "%{http_code}\n" -X POST https://api.jieuni.space/api/dev/login
# 404
```

## 10. 자주 생기는 문제

- **인증서가 안 나온다**: `docker compose logs caddy`. 대부분 DNS가 아직 EIP를 가리키지 않거나 Cloudflare 프록시가 켜져 있다(회색 구름이어야 한다). Caddy는 간격을 늘려 가며 다시 시도하고 Let's Encrypt가 막히면 ZeroSSL로 넘어간다. `caddy-data` 볼륨을 지우면 처음부터 다시 받아 한도를 더 쓰므로 지우지 않는다.
- **백엔드가 계속 재시작한다**: `docker compose logs backend`에 `prod 필수 환경 변수가 없거나 비어 있다: [...]`가 있으면 그 이름을 `.env`에 채우고 8절.
- **서버 재부팅 뒤 백엔드가 한두 번 재시작한다**: 재부팅 때는 restart 정책이 `depends_on` 순서를 따르지 않아 DB보다 먼저 뜰 수 있다. 저절로 맞춰진다.
- **SSH가 안 된다**: 집 IP가 바뀌었을 가능성이 크다. EC2 → Security Groups → 인스턴스의 보안 그룹 → Edit inbound rules → SSH 규칙의 Source를 **My IP**로 다시 고르고 저장.
- **이미지를 못 받는다**: 패키지가 공개인지(3절 2단계), 태그가 있는지 Packages에서 확인.
- **구글 `redirect_uri_mismatch`**: 21단계 URI와 11절 명령이 보여 주는 `redirect_uri`를 글자 그대로 대조한다. 콘솔 변경은 반영까지 몇 분 걸릴 수 있다.
- **구글 `access_denied`(앱이 테스트 중)**: Audience가 Testing이고 그 계정이 테스트 사용자가 아니다(22단계).
- **사진 업로드가 CORS로 막힌다**: R2 CORS 정책(18단계)의 origin이 `https://jieuni.space`인지, `www`로 접속하지 않았는지.
- **사진이 안 보인다**: 응답 헤더의 CSP `img-src`에 `https://photos.jieuni.space`가 있는지(없으면 Vercel `PHOTO_PUBLIC_BASE_URL` 넣고 Redeploy), R2 Custom Domains가 Active인지.
- **공개해도 페이지가 안 바뀐다**: Vercel Logs에서 `POST /api/revalidate`가 401이면 `REVALIDATE_SECRET` 불일치, 호출 자체가 없으면 백엔드 로그의 `재생성 호출 실패`·`재생성 주소나 비밀이 없어`.

## 11. 운영 확인

프론트·사진·로그인을 바꾼 뒤(3절 15단계 이후, 비밀값 교체, 도메인 변경) 위에서부터 한 번씩.

**헤더와 라우트**(로컬에서):

```
curl -sI https://jieuni.space | grep -iE '^HTTP|content-security'
# HTTP/2 200, img-src에 https://photos.jieuni.space

curl -sI https://www.jieuni.space | grep -iE '^HTTP|location'
# 308, location: https://jieuni.space/

curl -s -X POST https://jieuni.space/api/revalidate
# {"message":"재생성 비밀이 맞지 않아요"} (401, Next 라우트)
```

마지막 명령이 403·404면 요청이 Next 라우트가 아니라 rewrites를 타고 백엔드로 간 것이다(백엔드는 CSRF 토큰이 없어 403). 그러면 재생성이 동작하지 않는다.

**구글 로그인**:

```
curl -s -o /dev/null -w '%{redirect_url}\n' https://jieuni.space/oauth2/authorization/google
# https://accounts.google.com/...&redirect_uri=https%3A%2F%2Fjieuni.space%2Flogin%2Foauth2%2Fcode%2Fgoogle...
```

`redirect_uri`는 `FRONTEND_URL`로 고정되어 있고 프록시 헤더와 무관하다. 이어서 브라우저에서 로그아웃 상태로 공간 저장 → 로그인 시트 → 구글 → 돌아와 저장이 이어서 끝나고 내 공간에 보이는지.

**쿠키**: 개발자 도구 → Application → Cookies → `https://jieuni.space`. `SESSION`은 Secure · HttpOnly · SameSite=Lax, `XSRF-TOKEN`은 Secure.

**재생성**: 관리자에서 공개된 공간의 요약을 고쳐 저장 → 그 공간 상세를 **한 번** 새로고침하면 바뀐 글이 보인다. Vercel → Logs에서 `POST /api/revalidate` 200, 백엔드 로그에 `재생성 호출 실패`·`재생성 주소나 비밀이 없어`가 없다(성공은 로그를 남기지 않는다).

**사진**: 관리자에서 사진 업로드 → 개발자 도구 Network에서 `<계정ID>.r2.cloudflarestorage.com`으로 가는 PUT이 200이고 CORS 오류가 없다 → 상세에 `photos.jieuni.space` 주소로 보인다. `curl -sI https://photos.jieuni.space/<키>`가 200 · `content-type: image/webp`.

**CSP**: 홈 · 피드 · 상세(임베드 있는 공간) · 프로필 · 내 공간 · 관리자 사진 업로드에서 Console에 `Content-Security-Policy` 위반이 없다.

**검색·공유**:

```
curl -s https://jieuni.space/places/<slug> | grep -oE '<(meta property="og:|link rel="canonical")[^>]*>'
curl -s https://jieuni.space/sitemap.xml | head
curl -s https://jieuni.space/robots.txt
```

og·canonical·sitemap 주소가 모두 `https://jieuni.space/...`. 공유 카드 이미지 주소가 200이고 1200×630. 카카오 [공유 디버거](https://developers.kakao.com/tool/debugger/sharing)와 Facebook [Sharing Debugger](https://developers.facebook.com/tools/debug/)로 미리보기.

**프리뷰와 vercel.app 주소**:
- `main`이 아닌 브랜치를 push하면 프리뷰 빌드가 통과한다(`NEXT_PUBLIC_SITE_URL` 없이).
- 프리뷰 응답 헤더에 `X-Robots-Tag: noindex`. 프리뷰에 Deployment Protection이 켜져 있으면 curl은 Vercel 로그인 화면을 받으므로, Vercel에 로그인한 브라우저의 Network에서 본다.
- 운영 배포의 `*.vercel.app` 주소에서도 canonical이 `https://jieuni.space/...`:
  ```
  curl -s https://<프로젝트>.vercel.app/places/<slug> | grep -o '<link rel="canonical"[^>]*>'
  ```

**서버**: 9절 명령.

## 12. 백업과 복원

매일 04:00 KST에 서버 cron이 `deploy/backup.sh`를 부른다. `db` 컨테이너 안에서 `pg_dump -Fc` → compose의 `backup` 서비스(rclone 공식 이미지)가 R2 비공개 버킷 `jieuni-backup`에 올리고 서버의 파일은 지운다. 보관은 버킷 수명 규칙(7일)이 맡는다. 덤프가 AWS 밖에 있어 계정이 닫혀도 남는다.

`spring_session`·`spring_session_attributes`는 데이터를 빼고 구조만 덤프한다. 세션 ID는 로그인 토큰이라 백업에 남기지 않는다. 복원하면 모두 로그아웃된 상태가 된다.

**처음 한 번**

1. R2 → **Create bucket**: 이름 `jieuni-backup`, Location Automatic. 커스텀 도메인·Public Development URL·CORS는 켜지 않는다(운영 버킷 `jieuni`는 공개 읽기라 덤프를 거기 두지 않는다).
2. 버킷 → **Settings → Object lifecycle rules → Add rule**: 이름 `expire-7d`, 버킷 전체, **Delete uploaded objects after 7 days**.
3. R2 → **Manage API tokens → Create API token**: 이름 `jieuni-backup`, **Object Read & Write**, **Apply to specific buckets only → `jieuni-backup`**. 운영 토큰(`jieuni-prod`)과 따로 둔다.
4. 서버 `.env`에 `BACKUP_S3_ACCESS_KEY`·`BACKUP_S3_SECRET_KEY`(3단계 값). 엔드포인트는 같은 계정이라 `S3_ENDPOINT`를 그대로 쓴다.
5. 로컬에서 바뀐 파일 복사: `scp -i <키>.pem deploy/docker-compose.yml deploy/backup.sh ubuntu@<EIP>:~/jieuni/deploy/`, 서버에서 `chmod +x ~/jieuni/deploy/backup.sh`.
6. 손으로 한 번: `~/jieuni/deploy/backup.sh`. 마지막 줄이 `… ok jieuni-<시각>.dump <크기>`면 된 것이다. 이어서 아래 "복원 확인"을 한 번.
7. cron(`crontab -e`). 서버 시간대가 UTC라 19:00 UTC = 04:00 KST:
   ```
   0 19 * * * $HOME/jieuni/deploy/backup.sh >> $HOME/jieuni/backup.log 2>&1
   ```
8. (선택) 성공 핑: [healthchecks.io](https://healthchecks.io) 같은 곳에 하루 주기 체크를 만들고 주소를 `.env`의 `BACKUP_PING_URL`에. 백업이 끝날 때마다 부르고, 하루 넘게 안 오면 그쪽이 메일을 보낸다. 비우면 부르지 않는다.
9. 서버 `.env` 전체를 비밀번호 관리자 등 서버 밖 한 곳에 보관한다(3절 10단계에서 이미 했다면 4·8단계 값을 넣어 갱신). R2 운영·백업 토큰, 구글 시크릿, `REVALIDATE_SECRET`, `POSTGRES_PASSWORD`는 서버에만 있어 서버를 잃으면 함께 사라진다. 덤프가 R2에 있어도 백업 토큰이 없으면 받을 수 없다. 이후 `.env`를 바꿀 때마다(8절) 사본도 갱신한다.

**주 1회 확인**

```
tail -n 7 ~/jieuni/backup.log
cd ~/jieuni/deploy && docker compose run --rm -T backup lsl r2:jieuni-backup
```

날마다 `ok` 한 줄, 버킷에 7개 안팎. `ok`가 없는 날은 그 위 줄이 오류다. 성공 핑을 넣었으면 그쪽 알림이 먼저 온다.

**복원(실제 사고)**

```
cd ~/jieuni/deploy
docker compose run --rm -T backup lsf r2:jieuni-backup          # 파일 고르기
docker compose run --rm -T backup copy r2:jieuni-backup/<파일> /data/
docker compose stop backend
docker compose exec -T db sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner --exit-on-error' < backups/<파일>
docker compose up -d backend
rm -f backups/<파일>
```

`--clean --if-exists`는 덤프에 있는 테이블을 지우고 다시 만들어 채운다. 복원하는 동안 `backend`를 멈춰 두는 것은 그사이 쓰기가 섞이지 않게 하려는 것이다. 볼륨(`db-data`)째 잃었으면 `docker compose up -d db`가 빈 DB를 새로 만들고, 같은 명령으로 채운 뒤 `backend`를 띄운다.

**복원 확인(처음 한 번, 이후 분기마다)**

임시 컨테이너에 최신 덤프를 복원해 테이블별 행 수를 운영 DB와 비교한다. 운영 DB는 건드리지 않는다. 백업 직후에 해야 차이가 없다.

```
cd ~/jieuni/deploy && ./backup.sh
F=$(docker compose run --rm -T backup lsf r2:jieuni-backup | sort | tail -1)
docker compose run --rm -T backup copy "r2:jieuni-backup/$F" /data/
docker run -d --name jieuni-restore -e POSTGRES_USER=jieuni -e POSTGRES_DB=jieuni -e POSTGRES_PASSWORD=restore postgres:18
until docker exec jieuni-restore pg_isready -U jieuni -d jieuni; do sleep 1; done; sleep 2
docker exec -i jieuni-restore pg_restore -U jieuni -d jieuni --no-owner --exit-on-error < "backups/$F"

Q="select table_name, (xpath('/row/c/text()', query_to_xml(format('select count(*) as c from public.%I', table_name), false, true, '')))[1]::text from information_schema.tables where table_schema = 'public' and table_type = 'BASE TABLE' and table_name not like 'spring\_session%' order by 1"
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At -c "$1"' _ "$Q" > /tmp/live.txt
docker exec jieuni-restore psql -U jieuni -d jieuni -At -c "$Q" > /tmp/restored.txt
diff /tmp/live.txt /tmp/restored.txt && echo "diff 없음"

docker rm -f jieuni-restore && rm -f "backups/$F"
```

`diff 없음`이면 된 것이다. 세션 테이블은 덤프에 데이터가 없으니 비교에서 뺀다. 날짜와 결과를 PLAN D3 메모에 적는다.

**AWS 계정이 닫히거나 서버를 잃으면**

덤프·사진(R2)·프론트(Vercel)·DNS(Cloudflare)는 AWS와 무관해 그대로다. 새 계정(또는 새 인스턴스)에서 3절 3~10단계(10단계의 `.env`는 새로 채우지 않고 "처음 한 번" 9단계에서 서버 밖에 보관한 사본을 그대로 쓴다. 토큰·시크릿이 그대로라 R2·구글·Vercel 쪽은 바꿀 것이 없다) → `docker compose up -d db` → 위 "복원"의 받기·`pg_restore` → `docker compose up -d` → EIP가 바뀌었으면 3절 11~12단계로 `api` 레코드를 고친다. 2절 날짜 전에 Paid plan으로 올리는 것이 먼저다.
