package com.woo.server.domain.card.repository

import com.woo.server.common.enums.TagType
import com.woo.server.domain.card.entity.TransactionTag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 거래-태그 매핑 리포지토리
 *
 * 거래와 태그의 다대다 관계를 관리합니다.
 */
@Repository
interface TransactionTagRepository : JpaRepository<TransactionTag, Long> {

    /** 거래 ID로 매핑 목록 조회 (태그를 JOIN FETCH하여 N+1 방지) */
    @Query("SELECT tt FROM TransactionTag tt JOIN FETCH tt.tag WHERE tt.transaction.id = :transactionId")
    fun findByTransactionIdWithTag(transactionId: Long): List<TransactionTag>

    /** 여러 거래 ID로 매핑 목록 일괄 조회 (태그를 JOIN FETCH하여 N+1 방지) */
    @Query("SELECT tt FROM TransactionTag tt JOIN FETCH tt.tag WHERE tt.transaction.id IN :transactionIds")
    fun findByTransactionIdInWithTag(transactionIds: List<Long>): List<TransactionTag>

    /** 거래 ID로 매핑 목록 조회 */
    fun findByTransactionId(transactionId: Long): List<TransactionTag>

    /** 태그 ID로 매핑 목록 조회 */
    fun findByTagId(tagId: Long): List<TransactionTag>

    /** 거래-태그 매핑 삭제 */
    @Modifying
    fun deleteByTransactionIdAndTagId(transactionId: Long, tagId: Long)

    /** 태그 ID로 매핑 전체 삭제 */
    @Modifying
    fun deleteByTagId(tagId: Long)

    /** 거래-태그 매핑 존재 여부 확인 */
    fun existsByTransactionIdAndTagId(transactionId: Long, tagId: Long): Boolean

    /** 거래에 특정 유형의 태그가 존재하는지 확인 */
    fun existsByTransactionIdAndTagType(transactionId: Long, tagType: TagType): Boolean

    /** 거래의 MAIN 태그 조회 (JOIN FETCH) */
    @Query("SELECT tt FROM TransactionTag tt JOIN FETCH tt.tag WHERE tt.transaction.id = :transactionId AND tt.tagType = :tagType")
    fun findByTransactionIdAndTagType(
        @Param("transactionId") transactionId: Long,
        @Param("tagType") tagType: TagType
    ): List<TransactionTag>

    /**
     * 태그별 사용 금액 합계, 거래 건수를 집계합니다. (기간 필터)
     *
     * @return [tagId, tagName, tagColor, totalAmount, transactionCount] 배열 목록
     */
    @Query("""
        SELECT tt.tag.id, tt.tag.name, tt.tag.color,
               COALESCE(SUM(tt.transaction.amount), 0),
               COUNT(tt.transaction.id)
        FROM TransactionTag tt
        WHERE tt.transaction.transactionDate BETWEEN :startDate AND :endDate
        GROUP BY tt.tag.id, tt.tag.name, tt.tag.color
        ORDER BY COALESCE(SUM(tt.transaction.amount), 0) DESC
    """)
    fun sumAmountByTagAndPeriod(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>

    /**
     * 태그별 사용 금액 합계, 거래 건수를 집계합니다. (기간 + 태그 유형 필터)
     *
     * @return [tagId, tagName, tagColor, totalAmount, transactionCount] 배열 목록
     */
    @Query("""
        SELECT tt.tag.id, tt.tag.name, tt.tag.color,
               COALESCE(SUM(tt.transaction.amount), 0),
               COUNT(tt.transaction.id)
        FROM TransactionTag tt
        WHERE tt.transaction.transactionDate BETWEEN :startDate AND :endDate
        AND tt.tagType = :tagType
        GROUP BY tt.tag.id, tt.tag.name, tt.tag.color
        ORDER BY COALESCE(SUM(tt.transaction.amount), 0) DESC
    """)
    fun sumAmountByTagAndPeriodAndType(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        @Param("tagType") tagType: TagType
    ): List<Array<Any>>

    /**
     * 태그별·월별 사용 금액을 집계합니다. (Stacked Bar 차트용)
     *
     * @return [tagId, tagName, tagColor, yearMonth(yyyy-MM), totalAmount] 배열 목록
     */
    @Query(
        value = """
            SELECT t.id AS tag_id, t.name AS tag_name, t.color AS tag_color,
                   TO_CHAR(ct.transaction_date, 'YYYY-MM') AS year_month,
                   COALESCE(SUM(ct.amount), 0) AS total_amount
            FROM aguagu.transaction_tags tt
            JOIN aguagu.tags t ON tt.tag_id = t.id
            JOIN aguagu.card_transactions ct ON tt.transaction_id = ct.id
            WHERE ct.transaction_date BETWEEN :startDate AND :endDate
            GROUP BY t.id, t.name, t.color, TO_CHAR(ct.transaction_date, 'YYYY-MM')
            ORDER BY t.id, year_month
        """,
        nativeQuery = true
    )
    fun sumAmountByTagAndMonth(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>

    /**
     * 태그별·월별 사용 금액을 집계합니다. (태그 유형 필터, Stacked Bar 차트용)
     *
     * @return [tagId, tagName, tagColor, yearMonth(yyyy-MM), totalAmount] 배열 목록
     */
    @Query(
        value = """
            SELECT t.id AS tag_id, t.name AS tag_name, t.color AS tag_color,
                   TO_CHAR(ct.transaction_date, 'YYYY-MM') AS year_month,
                   COALESCE(SUM(ct.amount), 0) AS total_amount
            FROM aguagu.transaction_tags tt
            JOIN aguagu.tags t ON tt.tag_id = t.id
            JOIN aguagu.card_transactions ct ON tt.transaction_id = ct.id
            WHERE ct.transaction_date BETWEEN :startDate AND :endDate
            AND tt.tag_type = :tagType
            GROUP BY t.id, t.name, t.color, TO_CHAR(ct.transaction_date, 'YYYY-MM')
            ORDER BY t.id, year_month
        """,
        nativeQuery = true
    )
    fun sumAmountByTagAndMonthAndType(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        @Param("tagType") tagType: String
    ): List<Array<Any>>

    /**
     * 선택한 태그 ID 목록에 대한 금액 합계, 거래 건수를 집계합니다. (태그 유형 필터)
     *
     * @return [tagId, tagName, tagColor, totalAmount, transactionCount] 배열 목록
     */
    @Query("""
        SELECT tt.tag.id, tt.tag.name, tt.tag.color,
               COALESCE(SUM(tt.transaction.amount), 0),
               COUNT(tt.transaction.id)
        FROM TransactionTag tt
        WHERE tt.transaction.transactionDate BETWEEN :startDate AND :endDate
        AND tt.tagType = :tagType
        AND tt.tag.id IN :tagIds
        GROUP BY tt.tag.id, tt.tag.name, tt.tag.color
        ORDER BY COALESCE(SUM(tt.transaction.amount), 0) DESC
    """)
    fun sumAmountByTagIdsAndPeriod(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        @Param("tagIds") tagIds: List<Long>,
        @Param("tagType") tagType: TagType
    ): List<Array<Any>>

    /**
     * 선택한 태그 ID 목록에 대한 월별 금액을 집계합니다. (태그 유형 필터, Line 차트용)
     *
     * @return [tagId, tagName, tagColor, yearMonth(yyyy-MM), totalAmount] 배열 목록
     */
    @Query(
        value = """
            SELECT t.id AS tag_id, t.name AS tag_name, t.color AS tag_color,
                   TO_CHAR(ct.transaction_date, 'YYYY-MM') AS year_month,
                   COALESCE(SUM(ct.amount), 0) AS total_amount
            FROM aguagu.transaction_tags tt
            JOIN aguagu.tags t ON tt.tag_id = t.id
            JOIN aguagu.card_transactions ct ON tt.transaction_id = ct.id
            WHERE ct.transaction_date BETWEEN :startDate AND :endDate
            AND tt.tag_type = :tagType
            AND t.id IN (:tagIds)
            GROUP BY t.id, t.name, t.color, TO_CHAR(ct.transaction_date, 'YYYY-MM')
            ORDER BY t.id, year_month
        """,
        nativeQuery = true
    )
    fun sumAmountByTagIdsAndMonth(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        @Param("tagIds") tagIds: List<Long>,
        @Param("tagType") tagType: String
    ): List<Array<Any>>
}
