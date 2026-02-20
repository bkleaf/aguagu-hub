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
 * 카드 거래 내역의 CRUD 및 조회 기능을 제공합니다.
 */
@Repository
interface CardTransactionRepository : JpaRepository<CardTransaction, Long> {

    /**
     * 파싱 성공 여부로 거래 내역을 조회합니다.
     *
     * @param parseSuccess 파싱 성공 여부
     * @return 해당 조건에 맞는 거래 내역 목록
     */
    fun findByParseSuccess(parseSuccess: Boolean): List<CardTransaction>

    /**
     * 파싱 실패한 거래 내역만 조회합니다.
     *
     * @return 파싱 실패 거래 내역 목록
     */
    fun findByParseSuccessFalse(): List<CardTransaction>

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
        AND ct.parseSuccess = true
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
     * 파싱 성공한 거래 내역만 최근순으로 조회합니다.
     *
     * @return 파싱 성공 거래 내역 목록 (거래일시 내림차순)
     */
    fun findByParseSuccessTrueOrderByTransactionDateDesc(): List<CardTransaction>

    @Query("""
        SELECT COALESCE(SUM(ct.amount), 0)
        FROM CardTransaction ct
        WHERE ct.cardCompany = :cardCompany
        AND ct.cardLastFourDigits = :cardLastFourDigits
        AND ct.transactionDate BETWEEN :startDate AND :endDate
        AND ct.parseSuccess = true
    """)
    fun sumAmountByCardAndPeriod(
        @Param("cardCompany") cardCompany: CardCompany,
        @Param("cardLastFourDigits") cardLastFourDigits: String,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): BigDecimal
}
