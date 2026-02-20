package com.woo.server.domain.card.repository

import com.woo.server.domain.card.entity.AutoPayment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

/**
 * 자동결제 리포지토리
 */
@Repository
interface AutoPaymentRepository : JpaRepository<AutoPayment, Long> {

    /** 특정 신용카드의 자동결제 목록을 조회합니다. */
    fun findByCreditCardId(creditCardId: String): List<AutoPayment>

    /** 특정 신용카드의 활성 자동결제 목록을 조회합니다. */
    fun findByCreditCardIdAndActiveTrue(creditCardId: String): List<AutoPayment>

    /** 활성 상태인 전체 자동결제 목록을 조회합니다. */
    fun findByActiveTrue(): List<AutoPayment>

    /** 특정 신용카드의 활성 자동결제 금액 합계를 조회합니다. */
    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM AutoPayment a WHERE a.creditCard.id = :creditCardId AND a.active = true")
    fun sumAmountByCreditCardIdAndActiveTrue(@Param("creditCardId") creditCardId: String): BigDecimal
}
