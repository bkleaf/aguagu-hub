package com.woo.server.common.notification

import com.woo.server.common.config.TelegramProperties
import com.woo.server.domain.card.entity.CardMonthlyBill
import com.woo.server.domain.card.entity.CardParseFailure
import com.woo.server.domain.card.entity.CardTransaction
import com.woo.server.domain.card.service.LimitInfo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class TelegramNotificationService(
    private val telegramProperties: TelegramProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.create()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

    /**
     * 파싱 성공한 거래 알림 전송
     * @param monthlyUsage 한도 미설정 카드의 월간 사용액 (한도가 있으면 null)
     */
    fun notifyTransaction(transaction: CardTransaction, limitInfo: LimitInfo? = null, monthlyUsage: BigDecimal? = null) {
        if (!telegramProperties.enabled) {
            log.debug("텔레그램 알림 비활성화 상태")
            return
        }

        val message = buildSuccessMessage(transaction, limitInfo, monthlyUsage)
        sendMessage(message)
    }

    /**
     * 월별 청구서 알림 전송
     */
    fun notifyMonthlyBill(bill: CardMonthlyBill) {
        if (!telegramProperties.enabled) {
            log.debug("텔레그램 알림 비활성화 상태")
            return
        }

        val message = buildBillMessage(bill)
        sendMessage(message)
    }

    /**
     * 결제 취소 알림 전송
     */
    fun notifyCancellation(transaction: CardTransaction) {
        if (!telegramProperties.enabled) {
            log.debug("텔레그램 알림 비활성화 상태")
            return
        }

        val message = buildCancelMessage(transaction)
        sendMessage(message)
    }

    /**
     * 파싱 실패 알림 전송
     */
    fun notifyParseFailure(failure: CardParseFailure) {
        if (!telegramProperties.enabled) {
            log.debug("텔레그램 알림 비활성화 상태")
            return
        }

        val message = buildFailMessage(failure)
        sendMessage(message)
    }

    private fun buildSuccessMessage(tx: CardTransaction, limitInfo: LimitInfo?, monthlyUsage: BigDecimal? = null): String {
        val amount = tx.amount?.let { numberFormat.format(it) + "원" } ?: "알 수 없음"
        val date = tx.transactionDate?.format(dateFormatter) ?: "알 수 없음"
        val merchant = tx.merchantName ?: "알 수 없음"
        // 정산 기간 기준 누적 사용액 (한도 정보가 있으면 정산 기간 합산액, 월간 사용액이 있으면 그것, 없으면 카드사 원본 누적금액)
        val accumulated = (limitInfo?.usedAmount ?: monthlyUsage ?: tx.accumulatedAmount)?.let { numberFormat.format(it) + "원" }

        return buildString {
            appendLine("💳 카드 결제 알림")
            appendLine("━━━━━━━━")
            appendLine("카드사: ${tx.cardCompany.displayName}")
            if (tx.cardLastFourDigits != null) {
                appendLine("카드번호: ****${tx.cardLastFourDigits}")
            }
            appendLine("금액: $amount")
            appendLine("사용처: $merchant")
            appendLine("일시: $date")
            if (accumulated != null) {
                appendLine("누적: $accumulated")
            }
            if (limitInfo != null) {
                appendLine("━━━━━━━━")
                appendLine("${limitInfo.limitType.displayName} 한도: ${numberFormat.format(limitInfo.limitAmount)}원")
                appendLine("${limitInfo.limitType.displayName} 사용: ${numberFormat.format(limitInfo.usedAmount)}원")
                if (limitInfo.remaining < BigDecimal.ZERO) {
                    appendLine("⚠️ 한도 초과: ${numberFormat.format(limitInfo.remaining.negate())}원")
                } else {
                    appendLine("잔여 한도: ${numberFormat.format(limitInfo.remaining)}원")
                }
            } else if (monthlyUsage != null) {
                appendLine("━━━━━━━━")
                appendLine("월간 사용: ${numberFormat.format(monthlyUsage)}원")
            }
        }
    }

    private fun buildBillMessage(bill: CardMonthlyBill): String {
        val amount = numberFormat.format(bill.billingAmount) + "원"
        val billingDate = bill.billingDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val referenceDate = bill.referenceDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        return buildString {
            appendLine("📋 월별 청구서 알림")
            appendLine("━━━━━━━━")
            appendLine("카드사: ${bill.cardCompany.displayName}")
            appendLine("결제일: $billingDate")
            appendLine("청구금액: $amount")
            appendLine("기준일: $referenceDate")
        }
    }

    private fun buildCancelMessage(tx: CardTransaction): String {
        val amount = numberFormat.format(tx.amount) + "원"
        val date = tx.transactionDate.format(dateFormatter)

        return buildString {
            appendLine("❌ 카드 결제 취소")
            appendLine("━━━━━━━━")
            appendLine("카드사: ${tx.cardCompany.displayName}")
            if (tx.cardLastFourDigits != null) {
                appendLine("카드번호: ****${tx.cardLastFourDigits}")
            }
            appendLine("금액: $amount")
            appendLine("사용처: ${tx.merchantName}")
            appendLine("일시: $date")
        }
    }

    private fun buildFailMessage(failure: CardParseFailure): String {
        return buildString {
            appendLine("⚠️ 카드 문자 파싱 실패")
            appendLine("━━━━━━━━")
            appendLine("카드사: ${failure.cardCompany.displayName}")
            appendLine("사유: ${failure.failReason}")
            appendLine("원문: ${failure.rawMessage}")
        }
    }

    private fun sendMessage(text: String) {
        try {
            val url = "https://api.telegram.org/bot${telegramProperties.botToken}/sendMessage"
            restClient.post()
                .uri(url)
                .body(mapOf("chat_id" to telegramProperties.chatId, "text" to text))
                .retrieve()
                .toBodilessEntity()
            log.info("텔레그램 알림 전송 완료")
        } catch (e: Exception) {
            log.error("텔레그램 알림 전송 실패: ${e.message}", e)
        }
    }
}
