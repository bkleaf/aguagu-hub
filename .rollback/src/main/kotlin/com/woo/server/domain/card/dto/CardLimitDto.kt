package com.woo.server.domain.card.dto

import com.woo.server.common.enums.CardCompany
import com.woo.server.domain.card.entity.CardLimit
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "카드 한도 설정 요청")
data class CardLimitRequest(
    @Schema(description = "카드사", example = "SAMSUNG")
    @field:NotNull(message = "카드사는 필수입니다")
    val cardCompany: CardCompany,

    @Schema(description = "카드 끝 4자리", example = "9684")
    @field:NotBlank(message = "카드 끝4자리는 필수입니다")
    val cardLastFourDigits: String,

    @Schema(description = "월간 한도 (원)", example = "1000000")
    @field:NotNull(message = "월간 한도는 필수입니다")
    @field:Positive(message = "월간 한도는 양수여야 합니다")
    val monthlyLimit: BigDecimal
)

@Schema(description = "카드 한도 응답")
data class CardLimitResponse(
    @Schema(description = "한도 ID", example = "1")
    val id: Long,
    @Schema(description = "카드사 코드", example = "SAMSUNG")
    val cardCompany: CardCompany,
    @Schema(description = "카드사 이름", example = "삼성카드")
    val cardCompanyName: String,
    @Schema(description = "카드 끝 4자리", example = "9684")
    val cardLastFourDigits: String,
    @Schema(description = "월간 한도 (원)", example = "1000000")
    val monthlyLimit: BigDecimal,
    @Schema(description = "이번 달 사용액 (원)", example = "350000")
    val monthlyUsed: BigDecimal,
    @Schema(description = "잔여 한도 (원)", example = "650000")
    val remaining: BigDecimal,
    @Schema(description = "생성일시", example = "2025-01-30T10:00:00")
    val createdAt: LocalDateTime,
    @Schema(description = "수정일시", example = "2025-01-30T15:30:00")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(entity: CardLimit, monthlyUsed: BigDecimal): CardLimitResponse {
            val remaining = entity.monthlyLimit - monthlyUsed
            return CardLimitResponse(
                id = entity.id!!,
                cardCompany = entity.cardCompany,
                cardCompanyName = entity.cardCompany.displayName,
                cardLastFourDigits = entity.cardLastFourDigits,
                monthlyLimit = entity.monthlyLimit,
                monthlyUsed = monthlyUsed,
                remaining = remaining,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}
