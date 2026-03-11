-- =====================================================
-- tags 테이블에 tag_type 컬럼 추가 마이그레이션
-- 실행 시점: 백엔드 코드 배포 전에 수동 실행
-- 목적: 태그 자체에 유형(MAIN/DETAIL)을 부여하여 관리
-- =====================================================

-- 1) tags 테이블에 tag_type 컬럼 추가 (기본값: DETAIL)
ALTER TABLE aguagu.tags
ADD COLUMN tag_type VARCHAR(10) NOT NULL DEFAULT 'DETAIL';

-- 2) 기존 데이터 마이그레이션:
--    transaction_tags에서 MAIN으로 가장 많이 사용된 태그 → MAIN 유형으로 설정
UPDATE aguagu.tags
SET tag_type = 'MAIN'
WHERE id IN (
    SELECT tag_id
    FROM aguagu.transaction_tags
    WHERE tag_type = 'MAIN'
    GROUP BY tag_id
    HAVING COUNT(*) >= 1
);
