package com.woo.server.domain.card.service

import com.woo.server.common.enums.CardCompany
import com.woo.server.domain.card.dto.CardLimitRequest
import com.woo.server.domain.card.dto.CardLimitResponse
import com.woo.server.domain.card.entity.CardLimit
import com.woo.server.domain.card.repository.CardLimitRepository
import com.woo.server.domain.card.repository.CardTransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

@Service
@Transactional(readOnly = true)
class CardLimitService(
    private val cardLimitRepository: CardLimitRepository,
    private val cardTransactionRepository: CardTransactionRepository
) {

    @Transactional
    fun createOrUpdate(request: CardLimitRequest): CardLimitResponse {
        val existing = cardLimitRepository.findByCardCompanyAndCardLastFourDigits(
            request.cardCompany, request.cardLastFourDigits
        )

        val cardLimit = if (existing != null) {
            existing.monthlyLimit = request.monthlyLimit
            existing.updatedAt = LocalDateTime.now()
            cardLimitRepository.save(existing)
        } else {
            cardLimitRepository.save(
                CardLimit(
                    cardCompany = request.cardCompany,
                    cardLastFourDigits = request.cardLastFourDigits,
                    monthlyLimit = request.monthlyLimit
                )
            )
        }

        val monthlyUsed = getMonthlyUsed(cardLimit.cardCompany, cardLimit.cardLastFourDigits)
        return CardLimitResponse.from(cardLimit, monthlyUsed)
    }

    fun getAll(): List<CardLimitResponse> {
        return cardLimitRepository.findAll().map { cardLimit ->
            val monthlyUsed = getMonthlyUsed(cardLimit.cardCompany, cardLimit.cardLastFourDigits)
            CardLimitResponse.from(cardLimit, monthlyUsed)
        }
    }

    fun getById(id: Long): CardLimitResponse? {
        val cardLimit = cardLimitRepository.findById(id).orElse(null) ?: return null
        val monthlyUsed = getMonthlyUsed(cardLimit.cardCompany, cardLimit.cardLastFourDigits)
        return CardLimitResponse.from(cardLimit, monthlyUsed)
    }

    @Transactional
    fun delete(id: Long): Boolean {
        if (!cardLimitRepository.existsById(id)) return false
        cardLimitRepository.deleteById(id)
        return true
    }

    fun getLimitInfo(cardCompany: CardCompany, cardLastFourDigits: String): LimitInfo? {
        val cardLimit = cardLimitRepository.findByCardCompanyAndCardLastFourDigits(
            cardCompany, cardLastFourDigits
        ) ?: return null

        val monthlyUsed = getMonthlyUsed(cardCompany, cardLastFourDigits)
        return LimitInfo(
            monthlyLimit = cardLimit.monthlyLimit,
            monthlyUsed = monthlyUsed,
            remaining = cardLimit.monthlyLimit - monthlyUsed
        )
    }

    private fun getMonthlyUsed(cardCompany: CardCompany, cardLastFourDigits: String): BigDecimal {
        val now = YearMonth.now()
        val startDate = now.atDay(1).atStartOfDay()
        val endDate = now.atEndOfMonth().atTime(23, 59, 59)
        return cardTransactionRepository.sumAmountByCardAndPeriod(
            cardCompany, cardLastFourDigits, startDate, endDate
        )
    }
}

data class LimitInfo(
    val monthlyLimit: BigDecimal,
    val monthlyUsed: BigDecimal,
    val remaining: BigDecimal
)
