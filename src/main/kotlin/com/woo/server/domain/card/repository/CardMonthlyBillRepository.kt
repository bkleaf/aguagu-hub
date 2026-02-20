package com.woo.server.domain.card.repository

import com.woo.server.common.enums.CardCompany
import com.woo.server.domain.card.entity.CardMonthlyBill
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 월별 청구서 리포지토리
 *
 * 월별 청구서의 CRUD 및 조회 기능을 제공합니다.
 */
@Repository
interface CardMonthlyBillRepository : JpaRepository<CardMonthlyBill, Long> {

    /**
     * 결제일 기준 월별 청구서를 조회합니다.
     *
     * @param startDate 시작일 (해당 월 1일)
     * @param endDate 종료일 (해당 월 말일)
     * @return 해당 월의 청구서 목록 (결제일 내림차순)
     */
    fun findByBillingDateBetweenOrderByBillingDateDesc(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<CardMonthlyBill>

    /**
     * 카드사 + 결제일 기준 월별 청구서를 조회합니다.
     *
     * @param cardCompany 카드사
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 해당 카드사의 청구서 목록
     */
    fun findByCardCompanyAndBillingDateBetweenOrderByBillingDateDesc(
        cardCompany: CardCompany,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<CardMonthlyBill>

    /**
     * 중복 청구서 확인
     *
     * @param cardCompany 카드사
     * @param billingDate 결제일
     * @param referenceDate 기준일
     * @return 중복 여부
     */
    fun existsByCardCompanyAndBillingDateAndReferenceDate(
        cardCompany: CardCompany,
        billingDate: LocalDate,
        referenceDate: LocalDate
    ): Boolean

    /**
     * 결제일 기준 월별 청구 합계를 조회합니다.
     *
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 청구 합계 금액
     */
    @Query("""
        SELECT COALESCE(SUM(b.billingAmount), 0)
        FROM CardMonthlyBill b
        WHERE b.billingDate BETWEEN :startDate AND :endDate
    """)
    fun sumBillingAmountByBillingDateBetween(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): BigDecimal

    /**
     * 전체 청구서를 결제일 내림차순으로 조회합니다.
     *
     * @return 전체 청구서 목록
     */
    fun findAllByOrderByBillingDateDesc(): List<CardMonthlyBill>
}
