package com.woo.server.domain.card.repository

import com.woo.server.common.enums.CardCompany
import com.woo.server.domain.card.entity.CardTransaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 카드 거래 내역 리포지토리
 *
 * 파싱에 성공한 카드 거래 내역의 CRUD 및 조회 기능을 제공합니다.
 * 파싱 실패 내역은 CardParseFailureRepository를 사용합니다.
 */
@Repository
interface CardTransactionRepository : JpaRepository<CardTransaction, Long> {

    /**
     * 카드사별 거래 내역을 조회합니다.
     *
     * @param cardCompany 카드사
     * @return 해당 카드사의 거래 내역 목록
     */
    fun findByCardCompany(cardCompany: CardCompany): List<CardTransaction>

    /**
     * 특정 기간 내의 거래 내역을 조회합니다.
     *
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 해당 기간의 거래 내역 목록
     */
    fun findByTransactionDateBetween(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<CardTransaction>

    /**
     * 중복 거래 확인을 위한 조회
     *
     * 동일한 카드사, 금액, 거래일시, 사용처를 가진 거래가 있는지 확인합니다.
     *
     * @param cardCompany 카드사
     * @param amount 금액
     * @param transactionDate 거래 일시
     * @param merchantName 사용처
     * @return 중복 거래 존재 여부
     */
    fun existsByCardCompanyAndAmountAndTransactionDateAndMerchantName(
        cardCompany: CardCompany,
        amount: BigDecimal,
        transactionDate: LocalDateTime,
        merchantName: String
    ): Boolean

    /**
     * 특정 기간 내 총 사용 금액을 조회합니다.
     *
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 총 사용 금액
     */
    @Query("""
        SELECT COALESCE(SUM(ct.amount), 0)
        FROM CardTransaction ct
        WHERE ct.transactionDate BETWEEN :startDate AND :endDate
    """)
    fun sumAmountByTransactionDateBetween(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): BigDecimal

    /**
     * 최근 거래 내역을 조회합니다.
     *
     * @return 최근 거래 내역 목록 (생성일시 내림차순)
     */
    fun findAllByOrderByCreatedAtDesc(): List<CardTransaction>

    /**
     * 최근 거래 내역을 거래일시 기준으로 조회합니다.
     *
     * @return 거래 내역 목록 (거래일시 내림차순)
     */
    fun findAllByOrderByTransactionDateDesc(): List<CardTransaction>

    @Query("""
        SELECT COALESCE(SUM(ct.amount), 0)
        FROM CardTransaction ct
        WHERE ct.cardCompany = :cardCompany
        AND ct.cardLastFourDigits = :cardLastFourDigits
        AND ct.transactionDate BETWEEN :startDate AND :endDate
    """)
    fun sumAmountByCardAndPeriod(
        @Param("cardCompany") cardCompany: CardCompany,
        @Param("cardLastFourDigits") cardLastFourDigits: String,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): BigDecimal

    /**
     * 카드 끝자리 여러 변형(원본 + 마스킹)으로 월간 사용액을 합산합니다.
     *
     * 하나카드처럼 문자에서 마스킹된 형태(7*2*)로 저장되는 경우,
     * 한도에 등록된 원본(7921)과 매칭하기 위해 IN 절을 사용합니다.
     */
    @Query("""
        SELECT COALESCE(SUM(ct.amount), 0)
        FROM CardTransaction ct
        WHERE ct.cardCompany = :cardCompany
        AND ct.cardLastFourDigits IN :variants
        AND ct.transactionDate BETWEEN :startDate AND :endDate
    """)
    fun sumAmountByCardAndPeriodWithVariants(
        @Param("cardCompany") cardCompany: CardCompany,
        @Param("variants") variants: List<String>,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): BigDecimal

    /**
     * 태그 미분류 거래의 총액을 조회합니다.
     *
     * transaction_tags 테이블에 매핑이 없는 거래의 금액을 합산합니다.
     */
    @Query(
        value = """
            SELECT COALESCE(SUM(ct.amount), 0)
            FROM aguagu.card_transactions ct
            WHERE ct.transaction_date BETWEEN :startDate AND :endDate
            AND NOT EXISTS (
                SELECT 1 FROM aguagu.transaction_tags tt WHERE tt.transaction_id = ct.id
            )
        """,
        nativeQuery = true
    )
    fun sumUntaggedAmountByPeriod(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): BigDecimal

    /**
     * 태그 미분류 거래의 건수를 조회합니다.
     *
     * transaction_tags 테이블에 매핑이 없는 거래 건수를 반환합니다.
     */
    @Query(
        value = """
            SELECT COUNT(ct.id)
            FROM aguagu.card_transactions ct
            WHERE ct.transaction_date BETWEEN :startDate AND :endDate
            AND NOT EXISTS (
                SELECT 1 FROM aguagu.transaction_tags tt WHERE tt.transaction_id = ct.id
            )
        """,
        nativeQuery = true
    )
    fun countUntaggedByPeriod(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Long
}
