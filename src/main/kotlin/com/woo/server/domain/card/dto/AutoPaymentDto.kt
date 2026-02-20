package com.woo.server.domain.card.dto

import com.woo.server.domain.card.entity.AutoPayment
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 자동결제 등록/수정 요청 DTO
 */
@Schema(description = "자동결제 등록/수정 요청")
data class AutoPaymentRequest(
    @Schema(description = "신용카드 ID (예: SAMSUNG_4300)", example = "SAMSUNG_4300")
    @field:NotBlank(message = "신용카드 ID는 필수입니다")
    val creditCardId: String,

    @Schema(description = "자동결제 설명 (예: 넷플릭스, SKT)", example = "넷플릭스")
    @field:NotBlank(message = "설명은 필수입니다")
    @field:Size(max = 100, message = "설명은 100자 이내여야 합니다")
    val description: String,

    @Schema(description = "결제 금액 (원)", example = "17000")
    @field:NotNull(message = "금액은 필수입니다")
    @field:Positive(message = "금액은 양수여야 합니다")
    val amount: BigDecimal,

    @Schema(description = "매월 결제일 (1~31)", example = "15")
    @field:NotNull(message = "결제일은 필수입니다")
    @field:Min(1, message = "결제일은 1 이상이어야 합니다")
    @field:Max(31, message = "결제일은 31 이하여야 합니다")
    val paymentDay: Int,

    @Schema(description = "활성화 여부", example = "true")
    val active: Boolean = true
)

/**
 * 자동결제 응답 DTO
 */
@Schema(description = "자동결제 응답")
data class AutoPaymentResponse(
    @Schema(description = "자동결제 ID", example = "1")
    val id: Long,
    @Schema(description = "신용카드 ID", example = "SAMSUNG_4300")
    val creditCardId: String,
    @Schema(description = "카드사 이름", example = "삼성카드")
    val cardCompanyName: String,
    @Schema(description = "카드 끝 4자리", example = "4300")
    val cardLastFourDigits: String,
    @Schema(description = "자동결제 설명", example = "넷플릭스")
    val description: String,
    @Schema(description = "결제 금액 (원)", example = "17000")
    val amount: BigDecimal,
    @Schema(description = "매월 결제일", example = "15")
    val paymentDay: Int,
    @Schema(description = "활성화 여부", example = "true")
    val active: Boolean,
    @Schema(description = "생성일시")
    val createdAt: LocalDateTime,
    @Schema(description = "수정일시")
    val updatedAt: LocalDateTime
) {
    companion object {
        /** 엔티티를 응답 DTO로 변환합니다. */
        fun from(entity: AutoPayment): AutoPaymentResponse {
            return AutoPaymentResponse(
                id = entity.id!!,
                creditCardId = entity.creditCard.id,
                cardCompanyName = entity.creditCard.cardCompany.displayName,
                cardLastFourDigits = entity.creditCard.lastFourDigits,
                description = entity.description,
                amount = entity.amount,
                paymentDay = entity.paymentDay,
                active = entity.active,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}
