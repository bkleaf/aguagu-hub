package com.woo.server.common.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * 텔레그램 봇 초기 설정
 *
 * 앱 시작 시 Webhook URL 등록과 봇 커맨드 메뉴를 설정합니다.
 */
@Component
class TelegramBotConfig(
    private val telegramProperties: TelegramProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.create()

    /** 앱 시작 시 텔레그램 봇 설정을 수행합니다. */
    @EventListener(ApplicationReadyEvent::class)
    fun initialize() {
        if (!telegramProperties.enabled) {
            log.info("텔레그램 봇 비활성화 상태 — 설정 건너뜀")
            return
        }

        registerWebhook()
        registerCommands()
    }

    /** Webhook URL을 텔레그램에 등록합니다. (HTTPS만 허용) */
    private fun registerWebhook() {
        if (telegramProperties.webhookUrl.isBlank()) {
            log.warn("텔레그램 webhook URL이 설정되지 않았습니다 — webhook 등록 건너뜀")
            return
        }

        if (!telegramProperties.webhookUrl.startsWith("https://")) {
            log.warn("텔레그램 Webhook은 HTTPS URL만 허용됩니다. 현재: ${telegramProperties.webhookUrl} — webhook 등록 건너뜀")
            return
        }

        try {
            val url = "${baseUrl()}/setWebhook"
            restClient.post()
                .uri(url)
                .body(mapOf("url" to telegramProperties.webhookUrl))
                .retrieve()
                .toBodilessEntity()
            log.info("텔레그램 Webhook 등록 완료: ${telegramProperties.webhookUrl}")
        } catch (e: Exception) {
            log.error("텔레그램 Webhook 등록 실패: ${e.message}", e)
        }
    }

    /** 봇 커맨드 메뉴를 등록합니다. (setMyCommands — 덮어쓰기 방식) */
    private fun registerCommands() {
        try {
            val url = "${baseUrl()}/setMyCommands"
            val commands = listOf(
                mapOf("command" to "total", "description" to "월간 사용 현황 조회"),
                mapOf("command" to "limit", "description" to "등록된 카드의 한도 현황 조회"),
                mapOf("command" to "card", "description" to "등록된 신용카드 목록 조회")
            )
            restClient.post()
                .uri(url)
                .body(mapOf("commands" to commands))
                .retrieve()
                .toBodilessEntity()
            log.info("텔레그램 봇 커맨드 등록 완료")
        } catch (e: Exception) {
            log.error("텔레그램 봇 커맨드 등록 실패: ${e.message}", e)
        }
    }

    private fun baseUrl() = "https://api.telegram.org/bot${telegramProperties.botToken}"
}
