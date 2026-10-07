-- 제보 하루 한도(사용자별 오늘 건수)와 내 제보 목록을 사용자·시각으로 찾는다.
CREATE INDEX idx_credit_suggestions_user_created ON credit_suggestions (user_id, created_at DESC);
CREATE INDEX idx_places_suggested_by_created ON places (suggested_by, created_at DESC) WHERE suggested_by IS NOT NULL;
