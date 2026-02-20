package com.woo.server.domain.card.dto

import com.woo.server.common.enums.CardCompany
import com.woo.server.domain.card.entity.CardTransaction
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "카드 거래 내역 응답")
data class CardTransactionResponse(
    @Schema(description = "거래 ID", example = "1")
    val id: Long,
    @Schema(description = "발신자 전화번호", example = "01012345678")
    val phoneNumber: String?,
    @Schema(description = "카드사 코드", example = "SAMSUNG")
    val cardCompany: CardCompany,
    @Schema(description = "카드사 이름", example = "삼성카드")
    val cardCompanyName: String,
    @Schema(description = "결제 금액 (원)", example = "4500")
    val amount: BigDecimal,
    @Schema(description = "거래 일시", example = "2025-01-15T17:52:00")
    val transactionDate: LocalDateTime,
    @Schema(description = "사용처 (가맹점명)", example = "버거킹건대입구역")
    val merchantName: String,
    @Schema(description = "누적 사용 금액 (원)", example = "773774")
    val accumulatedAmount: BigDecimal?,
    @Schema(description = "파싱 성공 여부", example = "true")
    val parseSuccess: Boolean,
    @Schema(description = "파싱 실패 사유", example = "null")
    val parseFailReason: String?,
    @Schema(description = "생성 일시", example = "2025-01-15T17:52:30")
    val createdAt: LocalDateTime
) {
    companion object {
        /**
         * Entity를 Response DTO로 변환
         *
         * @param entity CardTransaction 엔티티
         * @return CardTransactionResponse DTO
         */
        fun from(entity: CardTransaction): CardTransactionResponse {
            return CardTransactionResponse(
                id = entity.id!!,
                phoneNumber = entity.phoneNumber,
                cardCompany = entity.cardCompany,
                cardCompanyName = entity.cardCompany.displayName,
                amount = entity.amount,
                transactionDate = entity.transactionDate,
                merchantName = entity.merchantName,
                accumulatedAmount = entity.accumulatedAmount,
                parseSuccess = entity.parseSuccess,
                parseFailReason = entity.parseFailReason,
                createdAt = entity.createdAt
            )
        }
    }
}

@Schema(description = "카드 문자 처리 결과 응답")
data class CardMessageProcessResponse(
    @Schema(description = "처리 성공 여부", example = "true")
    val success: Boolean,
    @Schema(description = "처리 결과 메시지", example = "문자 메시지가 성공적으로 처리되었습니다")
    val message: String,
    @Schema(description = "저장된 거래 정보 (성공 시)")
    val transaction: CardTransactionResponse?
) {
    companion object {
        /**
         * 성공 응답 생성
         */
        fun success(transaction: CardTransactionResponse): CardMessageProcessResponse {
            return CardMessageProcessResponse(
                success = true,
                message = "문자 메시지가 성공적으로 처리되었습니다",
                transaction = transaction
            )
        }

        /**
         * 실패 응답 생성 (파싱 실패지만 저장됨)
         */
        fun parseFailed(transaction: CardTransactionResponse): CardMessageProcessResponse {
            return CardMessageProcessResponse(
                success = false,
                message = "문자 파싱에 실패했지만 원본 메시지가 저장되었습니다",
                transaction = transaction
            )
        }

        /**
         * 중복 거래 응답 생성
         */
        fun duplicate(): CardMessageProcessResponse {
            return CardMessageProcessResponse(
                success = false,
                message = "이미 동일한 거래가 존재합니다",
                transaction = null
            )
        }

        /**
         * 오류 응답 생성
         */
        fun error(errorMessage: String): CardMessageProcessResponse {
            return CardMessageProcessResponse(
                success = false,
                message = errorMessage,
                transaction = null
            )
        }
    }
}
