-- card_transactions 테이블에 취소 여부 컬럼 추가
-- 기존 데이터는 모두 cancelled = false로 설정
ALTER TABLE aguagu.card_transactions ADD COLUMN cancelled BOOLEAN NOT NULL DEFAULT FALSE;
