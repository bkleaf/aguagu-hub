package com.woo.server.common.notification

import com.woo.server.common.config.TelegramProperties
import io.swagger.v3.oas.annotations.Hidden
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient

/**
 * 텔레그램 Webhook 수신 컨트롤러
 *
 * 텔레그램이 전송하는 Update JSON을 수신하여 커맨드를 처리합니다.
 */
@Hidden
@RestController
@RequestMapping("/api/v1/telegram")
class TelegramWebhookController(
    private val telegramProperties: TelegramProperties,
    private val telegramCommandService: TelegramCommandService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.create()

    /**
     * 텔레그램 Webhook 엔드포인트
     *
     * 텔레그램이 보내는 Update 객체를 수신하여 커맨드를 처리합니다.
     * chatId가 설정된 값과 일치하는 경우에만 응답합니다.
     */
    @PostMapping("/webhook")
    fun handleWebhook(@RequestBody update: TelegramUpdate): ResponseEntity<Void> {
        val message = update.message ?: return ResponseEntity.ok().build()
        val chatId = message.chat?.id?.toString() ?: return ResponseEntity.ok().build()
        val text = message.text ?: return ResponseEntity.ok().build()

        // chatId 검증 — 설정된 chatId와 일치하는 경우에만 처리
        if (chatId != telegramProperties.chatId) {
            log.warn("허용되지 않은 chatId: {}", chatId)
            return ResponseEntity.ok().build()
        }

        log.info("텔레그램 커맨드 수신: {}", text)

        val response = telegramCommandService.handleCommand(text)
        if (response != null) {
            sendMessage(chatId, response)
        }

        return ResponseEntity.ok().build()
    }

    /** 텔레그램으로 메시지를 전송합니다. */
    private fun sendMessage(chatId: String, text: String) {
        try {
            val url = "https://api.telegram.org/bot${telegramProperties.botToken}/sendMessage"
            restClient.post()
                .uri(url)
                .body(mapOf("chat_id" to chatId, "text" to text))
                .retrieve()
                .toBodilessEntity()
            log.info("텔레그램 커맨드 응답 전송 완료")
        } catch (e: Exception) {
            log.error("텔레그램 커맨드 응답 전송 실패: ${e.message}", e)
        }
    }
}

/** 텔레그램 Update 객체 */
data class TelegramUpdate(
    val updateId: Long? = null,
    val message: TelegramMessage? = null
)

/** 텔레그램 메시지 객체 */
data class TelegramMessage(
    val messageId: Long? = null,
    val chat: TelegramChat? = null,
    val text: String? = null
)

/** 텔레그램 채팅 객체 */
data class TelegramChat(
    val id: Long? = null
)
