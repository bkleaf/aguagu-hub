-- 신용카드 테이블 생성
-- aguagu 스키마에 credit_cards 테이블을 생성합니다.
-- ddl-auto=validate이므로 수동으로 실행해야 합니다.

CREATE TABLE aguagu.credit_cards (
    id                VARCHAR(30)  PRIMARY KEY,
    card_company      VARCHAR(20)  NOT NULL,
    last_four_digits  VARCHAR(4)   NOT NULL,
    billing_start_day INT          NOT NULL DEFAULT 1,
    billing_end_day   INT          NOT NULL DEFAULT 31,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (card_company, last_four_digits)
);

COMMENT ON TABLE aguagu.credit_cards IS '등록된 신용카드 정보';
COMMENT ON COLUMN aguagu.credit_cards.id IS '카드 ID (예: SAMSUNG_4300)';
COMMENT ON COLUMN aguagu.credit_cards.card_company IS '카드사 코드';
COMMENT ON COLUMN aguagu.credit_cards.last_four_digits IS '카드 끝 4자리';
COMMENT ON COLUMN aguagu.credit_cards.billing_start_day IS '정산 시작일';
COMMENT ON COLUMN aguagu.credit_cards.billing_end_day IS '정산 종료일';
