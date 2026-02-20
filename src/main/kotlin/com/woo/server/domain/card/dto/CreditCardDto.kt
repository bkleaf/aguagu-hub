package com.woo.server.domain.card.dto

import com.woo.server.common.enums.CardCompany
import com.woo.server.domain.card.entity.CreditCard
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

/**
 * 신용카드 등록 요청 DTO
 */
@Schema(description = "신용카드 등록 요청")
data class CreditCardRequest(
    @Schema(description = "카드사", example = "SAMSUNG")
    @field:NotNull(message = "카드사는 필수입니다")
    val cardCompany: CardCompany,

    @Schema(description = "카드 끝 4자리", example = "4300")
    @field:NotBlank(message = "카드 끝4자리는 필수입니다")
    val lastFourDigits: String,

    @Schema(description = "정산 시작일 (1~31)", example = "1")
    @field:Min(1) @field:Max(31)
    val billingStartDay: Int = 1,

    @Schema(description = "정산 종료일 (1~31)", example = "31")
    @field:Min(1) @field:Max(31)
    val billingEndDay: Int = 31,

    @Schema(description = "카드 결제일 (매월 N일, 1~31)", example = "15")
    @field:Min(1) @field:Max(31)
    val paymentDay: Int? = null,

    @Schema(description = "사용 여부", example = "true")
    val active: Boolean = true
)

/**
 * 신용카드 응답 DTO
 */
@Schema(description = "신용카드 응답")
data class CreditCardResponse(
    @Schema(description = "신용카드 ID", example = "SAMSUNG_4300")
    val id: String,
    @Schema(description = "카드사 코드", example = "SAMSUNG")
    val cardCompany: CardCompany,
    @Schema(description = "카드사 이름", example = "삼성카드")
    val cardCompanyName: String,
    @Schema(description = "카드 끝 4자리", example = "4300")
    val lastFourDigits: String,
    @Schema(description = "정산 시작일", example = "1")
    val billingStartDay: Int,
    @Schema(description = "정산 종료일", example = "31")
    val billingEndDay: Int,
    @Schema(description = "카드 결제일 (매월 N일)", example = "15")
    val paymentDay: Int?,
    @Schema(description = "사용 여부", example = "true")
    val active: Boolean,
    @Schema(description = "생성일시")
    val createdAt: LocalDateTime,
    @Schema(description = "수정일시")
    val updatedAt: LocalDateTime
) {
    companion object {
        /** 엔티티를 응답 DTO로 변환합니다. */
        fun from(entity: CreditCard): CreditCardResponse {
            return CreditCardResponse(
                id = entity.id,
                cardCompany = entity.cardCompany,
                cardCompanyName = entity.cardCompany.displayName,
                lastFourDigits = entity.lastFourDigits,
                billingStartDay = entity.billingStartDay,
                billingEndDay = entity.billingEndDay,
                paymentDay = entity.paymentDay,
                active = entity.active,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}
