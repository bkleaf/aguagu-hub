package com.woo.server.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "telegram")
data class TelegramProperties(
    val botToken: String = "",
    val chatId: String = "",
    val enabled: Boolean = false,
    /** Webhook URL (예: https://yourdomain.com/api/v1/telegram/webhook) */
    val webhookUrl: String = ""
)
