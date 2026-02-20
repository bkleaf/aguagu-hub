package com.woo.server.domain.card.dto

import com.woo.server.common.enums.CardCompany
import com.woo.server.common.enums.LimitType
import com.woo.server.domain.card.entity.CardLimit
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 카드 한도 설정 요청 DTO
 *
 * creditCardId를 기반으로 한도를 설정합니다.
 */
@Schema(description = "카드 한도 설정 요청")
data class CardLimitRequest(
    @Schema(description = "신용카드 ID (예: SAMSUNG_4300)", example = "SAMSUNG_4300")
    @field:NotBlank(message = "신용카드 ID는 필수입니다")
    val creditCardId: String,

    @Schema(description = "한도 유형 (MONTHLY, YEARLY, CUSTOM)", example = "MONTHLY")
    @field:NotNull(message = "한도 유형은 필수입니다")
    val limitType: LimitType = LimitType.MONTHLY,

    @Schema(description = "한도 금액 (원)", example = "1000000")
    @field:NotNull(message = "한도 금액은 필수입니다")
    @field:Positive(message = "한도 금액은 양수여야 합니다")
    val limitAmount: BigDecimal,

    @Schema(description = "커스텀 기간 시작일 (limitType이 CUSTOM일 때 필수)", example = "2025-01-01")
    val customStartDate: LocalDate? = null,

    @Schema(description = "커스텀 기간 종료일 (limitType이 CUSTOM일 때 필수)", example = "2025-03-31")
    val customEndDate: LocalDate? = null
)

/**
 * 카드 한도 응답 DTO
 */
@Schema(description = "카드 한도 응답")
data class CardLimitResponse(
    @Schema(description = "한도 ID", example = "1")
    val id: Long,
    @Schema(description = "신용카드 ID", example = "SAMSUNG_4300")
    val creditCardId: String,
    @Schema(description = "카드사 코드", example = "SAMSUNG")
    val cardCompany: CardCompany,
    @Schema(description = "카드사 이름", example = "삼성카드")
    val cardCompanyName: String,
    @Schema(description = "카드 끝 4자리", example = "4300")
    val cardLastFourDigits: String,
    @Schema(description = "한도 유형", example = "MONTHLY")
    val limitType: LimitType,
    @Schema(description = "한도 유형 이름", example = "월간")
    val limitTypeName: String,
    @Schema(description = "한도 금액 (원)", example = "1000000")
    val limitAmount: BigDecimal,
    @Schema(description = "해당 기간 사용액 (원)", example = "350000")
    val usedAmount: BigDecimal,
    @Schema(description = "잔여 한도 (원)", example = "650000")
    val remaining: BigDecimal,
    @Schema(description = "커스텀 기간 시작일", example = "2025-01-01")
    val customStartDate: LocalDate? = null,
    @Schema(description = "커스텀 기간 종료일", example = "2025-03-31")
    val customEndDate: LocalDate? = null,
    @Schema(description = "생성일시", example = "2025-01-30T10:00:00")
    val createdAt: LocalDateTime,
    @Schema(description = "수정일시", example = "2025-01-30T15:30:00")
    val updatedAt: LocalDateTime
) {
    companion object {
        /** 엔티티를 응답 DTO로 변환합니다. */
        fun from(entity: CardLimit, creditCardId: String, usedAmount: BigDecimal): CardLimitResponse {
            val remaining = entity.limitAmount - usedAmount
            return CardLimitResponse(
                id = entity.id!!,
                creditCardId = creditCardId,
                cardCompany = entity.cardCompany,
                cardCompanyName = entity.cardCompany.displayName,
                cardLastFourDigits = entity.cardLastFourDigits,
                limitType = entity.limitType,
                limitTypeName = entity.limitType.displayName,
                limitAmount = entity.limitAmount,
                usedAmount = usedAmount,
                remaining = remaining,
                customStartDate = entity.customStartDate,
                customEndDate = entity.customEndDate,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}
