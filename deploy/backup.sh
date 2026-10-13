#!/usr/bin/env bash
# 운영 DB를 덤프해 R2 백업 버킷에 올린다. 서버 cron이 매일 부른다(docs/RUNBOOK.md).
set -euo pipefail
cd "$(dirname "$0")"

bucket=jieuni-backup
name=jieuni-$(date -u +%Y%m%dT%H%MZ).dump
file=backups/$name

mkdir -p backups
trap 'rm -f "$file"' EXIT

# 세션 ID는 로그인 토큰이라 데이터는 빼고 테이블 구조만 남긴다.
docker compose exec -T db sh -c \
  'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc --exclude-table-data="spring_session*"' > "$file"
docker compose run --rm -T backup copy "/data/$name" "r2:$bucket/"

echo "$(date -u +%FT%TZ) ok $name $(du -h "$file" | cut -f1)"

ping=$(grep '^BACKUP_PING_URL=' .env | cut -d= -f2- || true)
if [ -n "$ping" ]; then
  curl -fsS -m 10 --retry 3 -o /dev/null "$ping" || echo "성공 핑 실패"
fi
