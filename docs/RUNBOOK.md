# 운영 절차

운영 서버를 다루는 방법. 값(비밀번호·키·IP)은 여기 적지 않는다. 결정의 이유는 `docs/DECISIONS.md` 9절.

## 1. 구성

| 주소 | 어디 | 무엇 |
| --- | --- | --- |
| `jieuni.space` | Vercel | 프론트. `/api/**`·`/oauth2/**`·`/login/oauth2/**`를 `api.jieuni.space`로 넘긴다 |
| `api.jieuni.space` | EC2 한 대(서울, t4g.small) | `~/jieuni/deploy`의 compose: `caddy`(80·443, 인증서) → `backend`(8080) → `db`(5432) |
| `photos.jieuni.space` | R2 운영 버킷 `jieuni` | 사진 공개 읽기 |

- 8080·5432는 호스트에 열지 않는다. 밖에서는 Caddy의 80·443만 보인다.
- 백엔드 이미지는 `.github/workflows/release.yml`이 만든다: `ghcr.io/smosco/jieuni-backend:<짧은 sha>`, `main`이면 `latest`도. 서버는 빌드하지 않고 받기만 한다.
- 서버의 `deploy/.env`가 비밀값의 유일한 자리다. 이름은 `deploy/.env.example`.

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

1. Actions → Release → Run workflow(브랜치 선택). 끝나면 Packages에 `jieuni-backend`가 생긴다. 태그(짧은 sha)를 적어 둔다.
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

    - **새로 만드는 운영 비밀값**: `POSTGRES_PASSWORD`, `REVALIDATE_SECRET`. 개발 값을 쓰지 않고 각각 새로 만든다. `REVALIDATE_SECRET`은 D2에서 Vercel 환경 변수에 같은 값을 넣는다.
      ```
      openssl rand -hex 32
      ```
    - **D2 전까지 임시값**: 이 시점에는 운영 R2 토큰, 사진 도메인, 구글 운영 리디렉트가 아직 없다. 아래 값을 넣어 두고 D2에서 실제 값으로 바꾼 뒤 8절대로 다시 띄운다. 그동안 사진 업로드와 구글 로그인은 동작하지 않는다.
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

**Cloudflare**

11. 존 `jieuni.space` → DNS → Records → Add record: Type `A`, Name `api`, IPv4 `<EIP>`, Proxy status **끔(DNS only, 회색 구름)**.
12. 로컬에서 `dig +short api.jieuni.space`가 EIP를 돌려줄 때까지 기다린다.

**시작**

13. `cd ~/jieuni/deploy && docker compose up -d`, 이어서 `docker compose logs -f caddy`에서 인증서 발급(`certificate obtained successfully`)을 본다.
14. 9절 확인 명령.

## 4. 새 이미지 배포

`main`에 `backend/**`가 병합되면 Release가 `latest`를 새로 만든다. 서버에서:

```
cd ~/jieuni/deploy
docker compose pull backend && docker compose up -d backend
docker image prune -af
```

`prune -af`는 돌고 있지 않은 이미지를 모두 지운다. 되돌릴 때는 GHCR에서 다시 받으면 된다(7절).

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

**마이그레이션과 되돌리기**: Flyway 기본 `ignoreMigrationPatterns`가 `*:future`라 DB가 이전 이미지보다 앞선 마이그레이션을 갖고 있어도 이전 이미지는 멈추지 않고 뜬다. 위험은 파괴적 마이그레이션(열 삭제·이름 변경) 뒤에 되돌리는 경우다. 이전 코드가 없어진 열을 찾으면서 쿼리가 깨진다. 엔티티가 매핑한 열이면 `ddl-auto: validate`에서 시작부터 멈춘다. 그래서 열 삭제·이름 변경은 "새 열 추가 → 코드 전환 배포 → 다음 배포에서 옛 열 삭제" 두 단계로 나눈다.

## 8. 환경 변수 바꾸기

`.env`를 고친 뒤 `docker compose up -d backend`. compose가 바뀐 값을 보고 컨테이너를 다시 만든다. `restart`는 새 값을 읽지 않는다.

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

## 11. D2 이후 채울 것

- 구글 로그인 확인: `https://jieuni.space`에서 로그인을 누르면 가는 구글 주소의 `redirect_uri`가 `https://jieuni.space/login/oauth2/code/google`이어야 한다. 이 값은 `FRONTEND_URL`로 고정되어 있고 프록시 헤더와 무관하다.
- 로그인 뒤 `SESSION`·`XSRF-TOKEN` 쿠키 둘 다 `Secure`인지(브라우저 개발자 도구 → Application → Cookies).
- Vercel 프리뷰 배포에서 구글 로그인하면 운영(`FRONTEND_URL`)으로 돌아간다.
- 백업과 복원(D3).
