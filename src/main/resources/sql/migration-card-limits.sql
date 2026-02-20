-- 실행 순서: 1 → 2

-- 1. card_transactions에 카드 끝4자리 컬럼 추가
ALTER TABLE aguagu.card_transactions ADD COLUMN card_last_four_digits VARCHAR(4);

-- 2. card_limits 테이블 생성
CREATE TABLE aguagu.card_limits (
    id BIGSERIAL PRIMARY KEY,
    card_company VARCHAR(20) NOT NULL,
    card_last_four_digits VARCHAR(4) NOT NULL,
    monthly_limit NUMERIC(15,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_card_limit UNIQUE (card_company, card_last_four_digits)
);
