package com.woo.server.common.notification

import com.woo.server.domain.card.entity.CreditCard
import com.woo.server.domain.card.repository.CardLimitRepository
import com.woo.server.domain.card.repository.CreditCardRepository
import com.woo.server.domain.card.service.CardLimitService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

/**
 * 텔레그램 봇 커맨드 처리 서비스
 *
 * /한도, /카드 커맨드에 대한 응답 메시지를 생성합니다.
 */
@Service
class TelegramCommandService(
    private val cardLimitService: CardLimitService,
    private val creditCardRepository: CreditCardRepository,
    private val cardLimitRepository: CardLimitRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

    /**
     * 커맨드 텍스트를 처리하여 응답 메시지를 반환합니다.
     *
     * @return 응답 메시지, 알 수 없는 커맨드면 null
     */
    fun handleCommand(text: String): String? {
        return when {
            text.startsWith("/total") -> handleTotalCommand()
            text.startsWith("/limit") -> handleLimitCommand()
            text.startsWith("/card") -> handleCardCommand()
            text.startsWith("/start") -> "안녕하세요! 아구아구 카드 관리 봇입니다.\n\n/total - 월간 사용 현황 조회\n/limit - 한도 현황 조회\n/card - 등록된 카드 조회"
            else -> null
        }
    }

    /** /total 커맨드: 활성 카드별 월간 사용액과 전체 합계를 조회합니다. */
    private fun handleTotalCommand(): String {
        val activeCards = creditCardRepository.findByActiveTrue()

        if (activeCards.isEmpty()) {
            return "📊 등록된 활성 카드가 없습니다."
        }

        var totalUsage = BigDecimal.ZERO

        return buildString {
            appendLine("📊 월간 사용 현황")
            appendLine("━━━━━━━━")

            activeCards.forEach { card ->
                val usage = cardLimitService.getMonthlyUsage(card.cardCompany, card.lastFourDigits)
                totalUsage = totalUsage.add(usage)
                appendLine("💳 ${card.cardCompany.displayName} (${card.lastFourDigits}): ${numberFormat.format(usage)}원")
            }

            appendLine("━━━━━━━━")
            appendLine("합계: ${numberFormat.format(totalUsage)}원")
        }.trimEnd()
    }

    /** /한도 커맨드: 등록된 모든 한도 정보를 조회합니다. */
    private fun handleLimitCommand(): String {
        val limits = cardLimitService.getAll()

        if (limits.isEmpty()) {
            return "📊 등록된 한도가 없습니다."
        }

        return buildString {
            appendLine("📊 카드 한도 현황")
            appendLine("━━━━━━━━")

            limits.forEachIndexed { index, limit ->
                if (index > 0) appendLine()
                appendLine("💳 ${limit.cardCompanyName} (${limit.cardLastFourDigits}) [${limit.limitTypeName}]")
                if (limit.limitType.name == "CUSTOM" && limit.customStartDate != null) {
                    appendLine("  기간: ${limit.customStartDate} ~ ${limit.customEndDate}")
                }
                appendLine("  한도: ${numberFormat.format(limit.limitAmount)}원")
                appendLine("  사용: ${numberFormat.format(limit.usedAmount)}원")
                appendLine("  잔여: ${numberFormat.format(limit.remaining)}원")
            }
        }.trimEnd()
    }

    /** /카드 커맨드: 등록된 모든 신용카드 정보를 조회합니다. */
    private fun handleCardCommand(): String {
        val creditCards = creditCardRepository.findAll()

        if (creditCards.isEmpty()) {
            return "💳 등록된 카드가 없습니다."
        }

        return buildString {
            appendLine("💳 등록된 카드 목록")
            appendLine("━━━━━━━━")

            creditCards.forEachIndexed { index, card ->
                if (index > 0) appendLine()
                appendLine("${card.cardCompany.displayName} (${card.lastFourDigits})")
                appendLine("  정산기간: ${formatBillingPeriod(card)}")
                appendLine("  결제일: ${formatPaymentDay(card.paymentDay)}")

                val limit = cardLimitRepository.findByCardCompanyAndCardLastFourDigits(
                    card.cardCompany, card.lastFourDigits
                )
                if (limit != null) {
                    appendLine("  한도: ${limit.limitType.displayName} ${numberFormat.format(limit.limitAmount)}원")
                } else {
                    appendLine("  한도: 미설정")
                }
            }
        }.trimEnd()
    }

    /** 정산 기간을 문자열로 포맷합니다. */
    private fun formatBillingPeriod(card: CreditCard): String {
        val endDay = if (card.billingEndDay == 31) "말일" else "${card.billingEndDay}일"
        return "${card.billingStartDay}일 ~ $endDay"
    }

    /** 결제일을 문자열로 포맷합니다. */
    private fun formatPaymentDay(paymentDay: Int?): String {
        return if (paymentDay != null) "매월 ${paymentDay}일" else "미설정"
    }
}
