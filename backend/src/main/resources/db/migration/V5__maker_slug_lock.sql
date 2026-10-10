-- 만든 사람 프로필 주소가 처음 밖에 나간 시각. 있으면 slug를 바꾸거나 비울 수 없다(크레딧에서 빠져도 그대로).
ALTER TABLE makers ADD COLUMN slug_locked_at timestamptz;

-- 이미 공개된 공간의 크레딧에 있는 만든 사람은 그 공간이 처음 공개된 시각으로 잠근다.
UPDATE makers m
SET slug_locked_at = credited.first_published_at
FROM (SELECT c.maker_id, min(p.published_at) AS first_published_at
      FROM place_credits c
               JOIN places p ON p.id = c.place_id
      WHERE p.published_at IS NOT NULL
      GROUP BY c.maker_id) credited
WHERE m.id = credited.maker_id
  AND m.slug IS NOT NULL;
