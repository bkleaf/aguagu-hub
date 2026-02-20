package com.woo.server.domain.card.service

import com.woo.server.common.enums.CardCompany
import com.woo.server.common.enums.LimitType
import com.woo.server.domain.card.dto.CardLimitRequest
import com.woo.server.domain.card.dto.CardLimitResponse
import com.woo.server.domain.card.entity.CardLimit
import com.woo.server.domain.card.entity.CreditCard
import com.woo.server.domain.card.repository.CardLimitRepository
import com.woo.server.domain.card.repository.CardTransactionRepository
import com.woo.server.domain.card.repository.CreditCardRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * 카드 한도 관리 서비스
 *
 * 등록된 CreditCard를 기반으로 한도를 관리합니다.
 * 한도 유형(MONTHLY, YEARLY, CUSTOM)에 따라 기간별 사용액을 계산합니다.
 */
@Service
@Transactional(readOnly = true)
class CardLimitService(
    private val cardLimitRepository: CardLimitRepository,
    private val cardTransactionRepository: CardTransactionRepository,
    private val creditCardRepository: CreditCardRepository
) {

    /**
     * 카드 한도를 생성하거나 수정합니다.
     *
     * creditCardId로 등록된 CreditCard를 조회하여 카드사와 끝4자리를 가져옵니다.
     */
    @Transactional
    fun createOrUpdate(request: CardLimitRequest): CardLimitResponse {
        // CUSTOM 유형일 때 시작일/종료일 검증
        if (request.limitType == LimitType.CUSTOM) {
            requireNotNull(request.customStartDate) { "CUSTOM 유형은 시작일이 필수입니다" }
            requireNotNull(request.customEndDate) { "CUSTOM 유형은 종료일이 필수입니다" }
            require(!request.customStartDate.isAfter(request.customEndDate)) { "시작일은 종료일 이전이어야 합니다" }
        }

        val creditCard = creditCardRepository.findById(request.creditCardId)
            .orElseThrow { IllegalArgumentException("등록되지 않은 신용카드입니다: ${request.creditCardId}") }

        val existing = cardLimitRepository.findByCardCompanyAndCardLastFourDigits(
            creditCard.cardCompany, creditCard.lastFourDigits
        )

        val cardLimit = if (existing != null) {
            existing.limitType = request.limitType
            existing.limitAmount = request.limitAmount
            existing.customStartDate = request.customStartDate
            existing.customEndDate = request.customEndDate
            existing.updatedAt = LocalDateTime.now()
            cardLimitRepository.save(existing)
        } else {
            cardLimitRepository.save(
                CardLimit(
                    cardCompany = creditCard.cardCompany,
                    cardLastFourDigits = creditCard.lastFourDigits,
                    limitType = request.limitType,
                    limitAmount = request.limitAmount,
                    customStartDate = request.customStartDate,
                    customEndDate = request.customEndDate
                )
            )
        }

        val usedAmount = getUsedAmount(cardLimit, creditCard)
        return CardLimitResponse.from(cardLimit, creditCard.id, usedAmount)
    }

    /** 전체 한도를 조회합니다. */
    fun getAll(): List<CardLimitResponse> {
        return cardLimitRepository.findAll().map { cardLimit ->
            val creditCard = findCreditCard(cardLimit.cardCompany, cardLimit.cardLastFourDigits)
            val creditCardId = creditCard?.id ?: CreditCard.generateId(cardLimit.cardCompany, cardLimit.cardLastFourDigits)
            val usedAmount = if (creditCard != null) {
                getUsedAmount(cardLimit, creditCard)
            } else {
                getUsedAmountDefault(cardLimit)
            }
            CardLimitResponse.from(cardLimit, creditCardId, usedAmount)
        }
    }

    /** 단건 한도를 조회합니다. */
    fun getById(id: Long): CardLimitResponse? {
        val cardLimit = cardLimitRepository.findById(id).orElse(null) ?: return null
        val creditCard = findCreditCard(cardLimit.cardCompany, cardLimit.cardLastFourDigits)
        val creditCardId = creditCard?.id ?: CreditCard.generateId(cardLimit.cardCompany, cardLimit.cardLastFourDigits)
        val usedAmount = if (creditCard != null) {
            getUsedAmount(cardLimit, creditCard)
        } else {
            getUsedAmountDefault(cardLimit)
        }
        return CardLimitResponse.from(cardLimit, creditCardId, usedAmount)
    }

    /** 한도를 삭제합니다. */
    @Transactional
    fun delete(id: Long): Boolean {
        if (!cardLimitRepository.existsById(id)) return false
        cardLimitRepository.deleteById(id)
        return true
    }

    /**
     * 한도 미설정 카드의 월간 사용액을 조회합니다.
     *
     * 등록된 CreditCard가 있으면 정산기간(billingStartDay/billingEndDay) 기준,
     * 없으면 달력 월간 기준으로 사용액을 계산합니다.
     */
    fun getMonthlyUsage(cardCompany: CardCompany, cardLastFourDigits: String): BigDecimal {
        val creditCard = findCreditCard(cardCompany, cardLastFourDigits)
        val (startDate, endDate) = if (creditCard != null) {
            calculateBillingPeriod(creditCard.billingStartDay, creditCard.billingEndDay)
        } else {
            val now = YearMonth.now()
            Pair(now.atDay(1).atStartOfDay(), now.atEndOfMonth().atTime(23, 59, 59))
        }
        val variants = buildCardDigitVariants(cardLastFourDigits)
        return cardTransactionRepository.sumAmountByCardAndPeriodWithVariants(
            cardCompany, variants, startDate, endDate
        )
    }

    /**
     * 카드 한도 정보를 조회합니다.
     *
     * 거래에서 파싱된 카드 끝자리(마스킹 가능)로 한도를 찾습니다.
     */
    fun getLimitInfo(cardCompany: CardCompany, cardLastFourDigits: String): LimitInfo? {
        // 직접 매칭 시도
        var cardLimit = cardLimitRepository.findByCardCompanyAndCardLastFourDigits(
            cardCompany, cardLastFourDigits
        )

        // 직접 매칭 실패 시, 변형 목록으로 재시도
        if (cardLimit == null) {
            val variants = buildCardDigitVariants(cardLastFourDigits)
            if (variants.size > 1) {
                cardLimit = cardLimitRepository.findByCardCompanyAndCardLastFourDigitsIn(
                    cardCompany, variants
                )
            }
        }

        cardLimit ?: return null

        val creditCard = findCreditCard(cardCompany, cardLimit.cardLastFourDigits)
        val usedAmount = if (creditCard != null) {
            getUsedAmount(cardLimit, creditCard)
        } else {
            getUsedAmountDefault(cardLimit)
        }

        return LimitInfo(
            limitType = cardLimit.limitType,
            limitAmount = cardLimit.limitAmount,
            usedAmount = usedAmount,
            remaining = cardLimit.limitAmount - usedAmount
        )
    }

    /**
     * CreditCard 정보와 한도 유형에 따라 기간별 사용액을 조회합니다.
     */
    private fun getUsedAmount(cardLimit: CardLimit, creditCard: CreditCard): BigDecimal {
        val (startDate, endDate) = calculatePeriod(cardLimit, creditCard)
        val variants = buildCardDigitVariants(creditCard.lastFourDigits)
        return cardTransactionRepository.sumAmountByCardAndPeriodWithVariants(
            creditCard.cardCompany, variants, startDate, endDate
        )
    }

    /**
     * CreditCard가 없는 경우 한도 유형에 따른 기본 기간으로 사용액을 조회합니다.
     */
    private fun getUsedAmountDefault(cardLimit: CardLimit): BigDecimal {
        val (startDate, endDate) = calculatePeriodDefault(cardLimit)
        val variants = buildCardDigitVariants(cardLimit.cardLastFourDigits)
        return cardTransactionRepository.sumAmountByCardAndPeriodWithVariants(
            cardLimit.cardCompany, variants, startDate, endDate
        )
    }

    /**
     * 한도 유형에 따라 기간을 계산합니다.
     *
     * MONTHLY: CreditCard의 billingStartDay/billingEndDay 기반 정산 기간
     * YEARLY: 현재 년 1월 1일 ~ 12월 31일
     * CUSTOM: customStartDate ~ customEndDate
     */
    private fun calculatePeriod(cardLimit: CardLimit, creditCard: CreditCard): Pair<LocalDateTime, LocalDateTime> {
        return when (cardLimit.limitType) {
            LimitType.MONTHLY -> calculateBillingPeriod(creditCard.billingStartDay, creditCard.billingEndDay)
            LimitType.YEARLY -> calculateYearlyPeriod()
            LimitType.CUSTOM -> calculateCustomPeriod(cardLimit.customStartDate!!, cardLimit.customEndDate!!)
        }
    }

    /**
     * CreditCard가 없을 때 한도 유형에 따라 기본 기간을 계산합니다.
     */
    private fun calculatePeriodDefault(cardLimit: CardLimit): Pair<LocalDateTime, LocalDateTime> {
        return when (cardLimit.limitType) {
            LimitType.MONTHLY -> {
                val now = YearMonth.now()
                Pair(now.atDay(1).atStartOfDay(), now.atEndOfMonth().atTime(23, 59, 59))
            }
            LimitType.YEARLY -> calculateYearlyPeriod()
            LimitType.CUSTOM -> calculateCustomPeriod(cardLimit.customStartDate!!, cardLimit.customEndDate!!)
        }
    }

    /** 연간 기간을 계산합니다. (현재 년 1/1 ~ 12/31) */
    private fun calculateYearlyPeriod(): Pair<LocalDateTime, LocalDateTime> {
        val year = LocalDate.now().year
        return Pair(
            LocalDate.of(year, 1, 1).atStartOfDay(),
            LocalDate.of(year, 12, 31).atTime(23, 59, 59)
        )
    }

    /** 커스텀 기간을 계산합니다. */
    private fun calculateCustomPeriod(startDate: LocalDate, endDate: LocalDate): Pair<LocalDateTime, LocalDateTime> {
        return Pair(startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
    }

    /**
     * 정산 기간을 계산합니다.
     *
     * 예: billingStartDay=15, billingEndDay=14
     *   → 오늘이 1/20이면: 1/15 ~ 2/14
     *   → 오늘이 1/10이면: 12/15 ~ 1/14
     */
    internal fun calculateBillingPeriod(billingStartDay: Int, billingEndDay: Int): Pair<LocalDateTime, LocalDateTime> {
        val today = LocalDate.now()

        if (billingStartDay == 1 && billingEndDay == 31) {
            val yearMonth = YearMonth.now()
            return Pair(
                yearMonth.atDay(1).atStartOfDay(),
                yearMonth.atEndOfMonth().atTime(23, 59, 59)
            )
        }

        val dayOfMonth = today.dayOfMonth

        val startDate: LocalDate
        val endDate: LocalDate

        if (billingStartDay <= billingEndDay) {
            if (dayOfMonth >= billingStartDay) {
                startDate = today.withDayOfMonth(minOf(billingStartDay, today.lengthOfMonth()))
                val endMonth = YearMonth.from(today)
                endDate = endMonth.atDay(minOf(billingEndDay, endMonth.lengthOfMonth()))
            } else {
                val prevMonth = YearMonth.from(today).minusMonths(1)
                startDate = prevMonth.atDay(minOf(billingStartDay, prevMonth.lengthOfMonth()))
                endDate = today.withDayOfMonth(minOf(billingEndDay, today.lengthOfMonth()))
            }
        } else {
            if (dayOfMonth >= billingStartDay) {
                startDate = today.withDayOfMonth(minOf(billingStartDay, today.lengthOfMonth()))
                val nextMonth = YearMonth.from(today).plusMonths(1)
                endDate = nextMonth.atDay(minOf(billingEndDay, nextMonth.lengthOfMonth()))
            } else {
                val prevMonth = YearMonth.from(today).minusMonths(1)
                startDate = prevMonth.atDay(minOf(billingStartDay, prevMonth.lengthOfMonth()))
                endDate = today.withDayOfMonth(minOf(billingEndDay, today.lengthOfMonth()))
            }
        }

        return Pair(startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
    }

    /** CreditCard를 카드사+끝4자리로 조회합니다. */
    private fun findCreditCard(cardCompany: CardCompany, lastFourDigits: String): CreditCard? {
        return creditCardRepository.findByCardCompanyAndLastFourDigits(cardCompany, lastFourDigits)
    }

    /**
     * 카드 끝자리의 가능한 변형 목록을 생성합니다.
     */
    internal fun buildCardDigitVariants(digits: String): List<String> {
        val variants = mutableListOf(digits)
        if (digits.length == 4 && digits.all { it.isDigit() }) {
            val masked = "${digits[0]}*${digits[2]}*"
            variants.add(masked)
        }
        return variants
    }
}

/** 한도 정보 */
data class LimitInfo(
    val limitType: LimitType,
    val limitAmount: BigDecimal,
    val usedAmount: BigDecimal,
    val remaining: BigDecimal
)
