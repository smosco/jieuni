-- 둘러보기(published·paused, published_at desc, id desc)용 인덱스를 부분 인덱스로 바꾼다.
-- status가 두 값이라 (status, published_at, id) 인덱스로는 전체 순서를 만들 수 없어 쓰이지 않았다.
DROP INDEX idx_places_status_published;

CREATE INDEX idx_places_listed_published ON places (published_at DESC, id DESC)
    WHERE status IN ('published', 'paused') AND slug IS NOT NULL;

-- 확인된(verified) 크레딧은 출처가 있어야 한다. 상태와 상관없이 항상 참인 형식 규칙이라 DB에도 둔다.
ALTER TABLE place_credits
    ADD CONSTRAINT ck_place_credits_verified_source CHECK (NOT verified OR source_url IS NOT NULL);

-- 한 번이라도 공개된 상태(published·paused·closed)는 공개 시각이 있어야 한다. 둘러보기 정렬·커서가 이 값에 기댄다.
ALTER TABLE places
    ADD CONSTRAINT ck_places_published_at CHECK (status NOT IN ('published', 'paused', 'closed') OR published_at IS NOT NULL);

-- 공간마다 사진 순서는 하나씩. 목록 쿼리가 position 0을 대표 사진으로 읽는다.
-- 순서를 맞바꿀 때 UPDATE 중간에 같은 position이 잠깐 겹치므로 커밋 때 검사한다(DEFERRABLE INITIALLY DEFERRED).
ALTER TABLE place_photos
    ADD CONSTRAINT uq_place_photos_position UNIQUE (place_id, position) DEFERRABLE INITIALLY DEFERRED;

-- V1의 (place_id, position) 인덱스는 위 UNIQUE 제약이 만든 인덱스와 컬럼이 같아 중복이다.
DROP INDEX idx_place_photos_place;
