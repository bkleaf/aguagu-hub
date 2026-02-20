package com.woo.server.domain.card.dto

import com.woo.server.common.enums.CardCompany
import com.woo.server.domain.card.entity.CardParseFailure
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 카드 문자 파싱 실패 내역 응답 DTO
 */
@Schema(description = "카드 문자 파싱 실패 내역 응답")
data class CardParseFailureResponse(
    @Schema(description = "ID")
    val id: Long,
    @Schema(description = "발신자 전화번호")
    val phoneNumber: String?,
    @Schema(description = "카드사 코드")
    val cardCompany: CardCompany,
    @Schema(description = "카드사 이름")
    val cardCompanyName: String,
    @Schema(description = "원본 문자 메시지")
    val rawMessage: String,
    @Schema(description = "파싱 실패 사유")
    val failReason: String,
    @Schema(description = "생성일시")
    val createdAt: LocalDateTime
) {
    companion object {
        /** 엔티티를 응답 DTO로 변환 */
        fun from(entity: CardParseFailure): CardParseFailureResponse {
            return CardParseFailureResponse(
                id = entity.id!!,
                phoneNumber = entity.phoneNumber,
                cardCompany = entity.cardCompany,
                cardCompanyName = entity.cardCompany.displayName,
                rawMessage = entity.rawMessage,
                failReason = entity.failReason,
                createdAt = entity.createdAt
            )
        }
    }
}
