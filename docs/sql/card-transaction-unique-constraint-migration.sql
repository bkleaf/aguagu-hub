-- 카드 거래 unique constraint에 cardLastFourDigits 추가
-- 기존: (cardCompany, amount, transactionDate, merchantName)
-- 변경: (cardCompany, cardLastFourDigits, amount, transactionDate, merchantName)
-- 같은 카드사의 다른 카드에서 동일 거래가 중복 처리되는 버그 수정

-- 기존 제약조건 삭제
ALTER TABLE aguagu.card_transactions DROP CONSTRAINT IF EXISTS uk_card_transaction_unique;

-- 새 제약조건 추가 (cardLastFourDigits 포함)
ALTER TABLE aguagu.card_transactions ADD CONSTRAINT uk_card_transaction_unique
    UNIQUE (card_company, card_last_four_digits, amount, transaction_date, merchant_name);
