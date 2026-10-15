#!/usr/bin/env bash
# 둘러보기 쿼리의 실행 계획을 V1 인덱스와 V3 부분 인덱스로 비교한다.
# 임시 postgres:18 컨테이너에 공간 2만 곳(목록 대상 1만 6천)을 넣고 EXPLAIN ANALYZE를 세 번씩 돌려 마지막 값을 보인다.
# 시간(ms)은 기계마다 다르고, 실행 계획의 모양(전: 전부 읽고 정렬 / 후: 인덱스 순서대로 25행)을 보는 것이 목적이다.
set -euo pipefail
cd "$(dirname "$0")/.."

migrations=backend/src/main/resources/db/migration
container="explain-browse-$$"

docker run -d --rm --name "$container" -e POSTGRES_PASSWORD=explain postgres:18 >/dev/null
trap 'docker stop "$container" >/dev/null' EXIT
until docker exec "$container" pg_isready -U postgres -q; do sleep 1; done
sleep 2

psql() { docker exec -i "$container" psql -U postgres -q -v ON_ERROR_STOP=1 "$@"; }

psql < "$migrations/V1__init.sql"
psql < "$migrations/V2__spring_session.sql"

# 70% 공개, 10% 휴업, 10% 폐업, 10% 작성 중. 공개 시각은 7분 간격.
psql <<'SQL'
insert into places (slug, name, kind, status, published_at, created_at, updated_at)
select 'p-' || g, '공간 ' || g,
  (array['cafe','bar','restaurant','stay','garden','museum','gallery','bookstore','shop','religious','public','other'])[1 + g % 12],
  case when g % 10 < 7 then 'published' when g % 10 = 7 then 'paused' when g % 10 = 8 then 'closed' else 'draft' end,
  case when g % 10 < 9 then timestamptz '2026-01-01' + g * interval '7 minutes' end,
  now(), now()
from generate_series(1, 20000) g;
analyze places;
SQL

# PlaceRepository.findTiles·findTilesAfter와 같은 모양. 커서는 목록의 8,000번째 행.
listed="from places p left join place_photos ph on ph.place_id = p.id and ph.position = 0
  where p.status in ('published','paused') and p.slug is not null
    and p.kind in ('cafe','bar','restaurant','stay','garden','museum','gallery','bookstore','shop','religious','public','other')"
select="select p.id, p.slug, p.name, p.region, p.kind, p.status, p.published_at, ph.id"
order="order by p.published_at desc, p.id desc limit 25"
IFS='|' read -r at id < <(psql -At -c "select p.published_at, p.id $listed order by p.published_at desc, p.id desc offset 7999 limit 1")

explain() {
  local plan
  for _ in 1 2 3; do plan=$(psql -At -c "explain analyze $1"); done
  echo "-- $2"
  grep -E 'Execution Time|on places|Sort  |Rows Removed by Filter' <<<"$plan" | sed -E 's/^ *//; s/\(cost=[^)]*\) //'
}

run_all() {
  echo "=== $1"
  explain "$select $listed $order" "첫 페이지"
  explain "$select $listed and (p.published_at < '$at' or (p.published_at = '$at' and p.id < $id)) $order" "8,000번째 뒤, or 조건"
  explain "$select $listed and (p.published_at, p.id) < ('$at', $id) $order" "8,000번째 뒤, 행 비교"
}

run_all "전: V1 인덱스 (status, published_at desc, id desc)"
psql < "$migrations/V3__places_listed_index_credit_source.sql"
psql -c "analyze places"
run_all "후: V3 부분 인덱스"
