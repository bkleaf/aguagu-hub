package com.woo.server.domain.card.service

import com.woo.server.domain.card.dto.AutoPaymentRequest
import com.woo.server.domain.card.dto.AutoPaymentResponse
import com.woo.server.domain.card.entity.AutoPayment
import com.woo.server.domain.card.repository.AutoPaymentRepository
import com.woo.server.domain.card.repository.CreditCardRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 자동결제 관리 서비스
 *
 * 카드별 정기 결제(넷플릭스, 보험료, 통신비 등)의 CRUD 및
 * 활성 자동결제 금액 합산 기능을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
class AutoPaymentService(
    private val autoPaymentRepository: AutoPaymentRepository,
    private val creditCardRepository: CreditCardRepository
) {

    /** 자동결제를 등록합니다. */
    @Transactional
    fun create(request: AutoPaymentRequest): AutoPaymentResponse {
        val creditCard = creditCardRepository.findById(request.creditCardId)
            .orElseThrow { IllegalArgumentException("등록되지 않은 신용카드입니다: ${request.creditCardId}") }

        val autoPayment = autoPaymentRepository.save(
            AutoPayment(
                creditCard = creditCard,
                description = request.description,
                amount = request.amount,
                paymentDay = request.paymentDay,
                active = request.active
            )
        )

        return AutoPaymentResponse.from(autoPayment)
    }

    /** 전체 자동결제를 조회합니다. */
    fun getAll(): List<AutoPaymentResponse> {
        return autoPaymentRepository.findAll().map { AutoPaymentResponse.from(it) }
    }

    /** 단건 자동결제를 조회합니다. */
    fun getById(id: Long): AutoPaymentResponse? {
        return autoPaymentRepository.findById(id).orElse(null)?.let { AutoPaymentResponse.from(it) }
    }

    /** 특정 신용카드의 자동결제를 조회합니다. */
    fun getByCreditCardId(creditCardId: String): List<AutoPaymentResponse> {
        return autoPaymentRepository.findByCreditCardId(creditCardId).map { AutoPaymentResponse.from(it) }
    }

    /** 자동결제를 수정합니다. */
    @Transactional
    fun update(id: Long, request: AutoPaymentRequest): AutoPaymentResponse {
        val autoPayment = autoPaymentRepository.findById(id)
            .orElseThrow { IllegalArgumentException("등록되지 않은 자동결제입니다: $id") }

        val creditCard = creditCardRepository.findById(request.creditCardId)
            .orElseThrow { IllegalArgumentException("등록되지 않은 신용카드입니다: ${request.creditCardId}") }

        autoPayment.description = request.description
        autoPayment.amount = request.amount
        autoPayment.paymentDay = request.paymentDay
        autoPayment.active = request.active
        autoPayment.updatedAt = LocalDateTime.now()

        return AutoPaymentResponse.from(autoPaymentRepository.save(autoPayment))
    }

    /** 자동결제를 삭제합니다. */
    @Transactional
    fun delete(id: Long): Boolean {
        if (!autoPaymentRepository.existsById(id)) return false
        autoPaymentRepository.deleteById(id)
        return true
    }

    /** 특정 신용카드의 활성 자동결제 금액 합계를 조회합니다. */
    fun getActiveAmountByCreditCard(creditCardId: String): BigDecimal {
        return autoPaymentRepository.sumAmountByCreditCardIdAndActiveTrue(creditCardId)
    }
}
