package com.woo.server.domain.card.service

import com.woo.server.common.enums.LimitType
import com.woo.server.domain.card.dto.CardUsageSummaryResponse
import com.woo.server.domain.card.dto.UsageSummaryOverview
import com.woo.server.domain.card.entity.CardLimit
import com.woo.server.domain.card.entity.CreditCard
import com.woo.server.domain.card.repository.CardLimitRepository
import com.woo.server.domain.card.repository.CardTransactionRepository
import com.woo.server.domain.card.repository.CreditCardRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 카드별 사용 현황 집계 서비스
 *
 * 등록된 활성 카드의 정산기간 기준 사용 금액, 한도 대비 사용률을 집계합니다.
 */
@Service
@Transactional(readOnly = true)
class CardUsageSummaryService(
    private val creditCardRepository: CreditCardRepository,
    private val cardLimitRepository: CardLimitRepository,
    private val cardTransactionRepository: CardTransactionRepository,
    private val cardLimitService: CardLimitService
) {

    /**
     * 전체 카드 사용 현황 요약을 조회합니다.
     *
     * 활성 카드 각각의 정산기간 사용액과 한도 정보를 집계하여 반환합니다.
     */
    fun getUsageSummary(): UsageSummaryOverview {
        val activeCards = creditCardRepository.findByActiveTrue()

        val cardSummaries = activeCards.map { card ->
            // 정산기간 계산
            val (periodStartDateTime, periodEndDateTime) = cardLimitService.calculateBillingPeriod(
                card.billingStartDay, card.billingEndDay
            )
            val periodStart = periodStartDateTime.toLocalDate()
            val periodEnd = periodEndDateTime.toLocalDate()

            // 마스킹 변형 생성 및 사용액 조회
            val variants = cardLimitService.buildCardDigitVariants(card.lastFourDigits)
            val usedAmount = cardTransactionRepository.sumAmountByCardAndPeriodWithVariants(
                card.cardCompany, variants, periodStartDateTime, periodEndDateTime
            )

            // 한도 조회
            val cardLimit = cardLimitRepository.findByCardCompanyAndCardLastFourDigits(
                card.cardCompany, card.lastFourDigits
            )

            val hasLimit = cardLimit != null
            val limitType = cardLimit?.limitType
            val limitTypeName = cardLimit?.limitType?.displayName
            val limitAmount = cardLimit?.limitAmount

            // 한도 기간 내 사용액 계산 (한도 유형별 기간 기준)
            val limitUsedAmount = if (cardLimit != null) {
                val (limitStart, limitEnd) = calculateLimitPeriod(cardLimit, card)
                cardTransactionRepository.sumAmountByCardAndPeriodWithVariants(
                    card.cardCompany, variants, limitStart, limitEnd
                )
            } else null

            val remaining = if (limitAmount != null && limitUsedAmount != null) limitAmount - limitUsedAmount else null
            val usagePercent = if (limitAmount != null && limitAmount > BigDecimal.ZERO && limitUsedAmount != null) {
                limitUsedAmount.multiply(BigDecimal(100))
                    .divide(limitAmount, 1, RoundingMode.HALF_UP)
                    .toDouble()
            } else null

            CardUsageSummaryResponse(
                creditCardId = card.id,
                cardCompany = card.cardCompany,
                cardCompanyName = card.cardCompany.displayName,
                lastFourDigits = card.lastFourDigits,
                billingStartDay = card.billingStartDay,
                billingEndDay = card.billingEndDay,
                periodStart = periodStart,
                periodEnd = periodEnd,
                usedAmount = usedAmount,
                hasLimit = hasLimit,
                limitType = limitType,
                limitTypeName = limitTypeName,
                limitAmount = limitAmount,
                limitUsedAmount = limitUsedAmount,
                remaining = remaining,
                usagePercent = usagePercent
            )
        }

        // 전체 합산 계산
        val totalUsedAmount = cardSummaries.fold(BigDecimal.ZERO) { acc, it -> acc + it.usedAmount }

        return UsageSummaryOverview(
            cards = cardSummaries,
            totalUsedAmount = totalUsedAmount
        )
    }

    /**
     * 한도 유형에 따른 기간을 계산합니다.
     *
     * MONTHLY: 신용카드의 정산기간 (billingStartDay/billingEndDay)
     * YEARLY: 현재 년 1/1 ~ 12/31
     * CUSTOM: 한도에 설정된 customStartDate ~ customEndDate
     */
    private fun calculateLimitPeriod(cardLimit: CardLimit, creditCard: CreditCard): Pair<LocalDateTime, LocalDateTime> {
        return when (cardLimit.limitType) {
            LimitType.MONTHLY -> cardLimitService.calculateBillingPeriod(
                creditCard.billingStartDay, creditCard.billingEndDay
            )
            LimitType.YEARLY -> {
                val year = LocalDate.now().year
                Pair(
                    LocalDate.of(year, 1, 1).atStartOfDay(),
                    LocalDate.of(year, 12, 31).atTime(23, 59, 59)
                )
            }
            LimitType.CUSTOM -> Pair(
                cardLimit.customStartDate!!.atStartOfDay(),
                cardLimit.customEndDate!!.atTime(23, 59, 59)
            )
        }
    }
}
