-- 신용카드 결제일 컬럼 추가 마이그레이션
ALTER TABLE aguagu.credit_cards ADD COLUMN payment_day INTEGER;
