-- =============================================
-- aguagu 스키마 및 테이블 생성 DDL
-- =============================================

CREATE SCHEMA IF NOT EXISTS aguagu;

CREATE TABLE IF NOT EXISTS aguagu.card_transactions (
    id                 BIGSERIAL PRIMARY KEY,
    phone_number       VARCHAR(20),
    card_company       VARCHAR(20)      NOT NULL,
    amount             NUMERIC(15, 2)   NOT NULL,
    transaction_date   TIMESTAMP        NOT NULL,
    merchant_name      VARCHAR(100)     NOT NULL,
    accumulated_amount NUMERIC(15, 2),
    raw_message        TEXT             NOT NULL,
    parse_success      BOOLEAN          NOT NULL DEFAULT TRUE,
    parse_fail_reason  VARCHAR(500),
    created_at         TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_card_transaction_date ON aguagu.card_transactions (transaction_date);
CREATE INDEX IF NOT EXISTS idx_card_company ON aguagu.card_transactions (card_company);
CREATE INDEX IF NOT EXISTS idx_parse_success ON aguagu.card_transactions (parse_success);

ALTER TABLE aguagu.card_transactions
    ADD CONSTRAINT uk_card_transaction_unique
        UNIQUE (card_company, amount, transaction_date, merchant_name);

-- =============================================
-- 기존 데이터 마이그레이션 (필요 시 수동 실행)
-- =============================================
-- INSERT INTO aguagu.card_transactions SELECT * FROM public.card_transactions;
