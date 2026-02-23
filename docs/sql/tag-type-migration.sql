-- =====================================================
-- 태그 Main/Detail 분리 DDL 마이그레이션
-- 실행 시점: 백엔드 코드 배포 전에 수동 실행
-- =====================================================

-- 1) tag_type 컬럼 추가 (기본값: DETAIL)
ALTER TABLE aguagu.transaction_tags
ADD COLUMN tag_type VARCHAR(10) NOT NULL DEFAULT 'DETAIL';

-- 2) 기존 데이터 마이그레이션: 각 거래의 첫 번째 태그를 MAIN으로 변경
UPDATE aguagu.transaction_tags tt
SET tag_type = 'MAIN'
WHERE tt.id IN (
    SELECT DISTINCT ON (transaction_id) id
    FROM aguagu.transaction_tags
    ORDER BY transaction_id, id ASC
);

-- 3) Partial unique index: 거래당 MAIN 태그 최대 1개 제약
CREATE UNIQUE INDEX uk_transaction_main_tag
ON aguagu.transaction_tags(transaction_id)
WHERE tag_type = 'MAIN';
