-- 카드 한도 유형 확장 마이그레이션
-- 기존 monthlyLimit을 limitAmount로 변경하고, limitType/커스텀 기간 컬럼 추가

ALTER TABLE aguagu.card_limits ADD COLUMN limit_type VARCHAR(20) NOT NULL DEFAULT 'MONTHLY';
ALTER TABLE aguagu.card_limits RENAME COLUMN monthly_limit TO limit_amount;
ALTER TABLE aguagu.card_limits ADD COLUMN custom_start_date DATE;
ALTER TABLE aguagu.card_limits ADD COLUMN custom_end_date DATE;
