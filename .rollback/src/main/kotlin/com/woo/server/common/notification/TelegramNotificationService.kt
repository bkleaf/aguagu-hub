package com.woo.server.common.notification

import com.woo.server.common.config.TelegramProperties
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

    fun notifyTransaction(transaction: CardTransaction, limitInfo: LimitInfo? = null) {
        if (!telegramProperties.enabled) {
            log.debug("텔레그램 알림 비활성화 상태")
            return
        }

        val message = if (transaction.parseSuccess) {
            buildSuccessMessage(transaction, limitInfo)
        } else {
            buildFailMessage(transaction)
        }

        sendMessage(message)
    }

    private fun buildSuccessMessage(tx: CardTransaction, limitInfo: LimitInfo?): String {
        val amount = tx.amount?.let { numberFormat.format(it) + "원" } ?: "알 수 없음"
        val date = tx.transactionDate?.format(dateFormatter) ?: "알 수 없음"
        val merchant = tx.merchantName ?: "알 수 없음"
        val accumulated = tx.accumulatedAmount?.let { numberFormat.format(it) + "원" }

        return buildString {
            appendLine("💳 카드 결제 알림")
            appendLine("━━━━━━━━━━━━━━━")
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
                appendLine("━━━━━━━━━━━━━━━")
                appendLine("월 한도: ${numberFormat.format(limitInfo.monthlyLimit)}원")
                appendLine("월 사용: ${numberFormat.format(limitInfo.monthlyUsed)}원")
                if (limitInfo.remaining < BigDecimal.ZERO) {
                    appendLine("⚠️ 한도 초과: ${numberFormat.format(limitInfo.remaining.negate())}원")
                } else {
                    appendLine("잔여 한도: ${numberFormat.format(limitInfo.remaining)}원")
                }
            }
        }
    }

    private fun buildFailMessage(tx: CardTransaction): String {
        return buildString {
            appendLine("⚠️ 카드 문자 파싱 실패")
            appendLine("━━━━━━━━━━━━━━━")
            appendLine("카드사: ${tx.cardCompany.displayName}")
            appendLine("사유: ${tx.parseFailReason ?: "알 수 없음"}")
            appendLine("원문: ${tx.rawMessage}")
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
