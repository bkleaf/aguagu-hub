package com.woo.server.domain.card.dto

import com.woo.server.common.enums.CardCompany
import com.woo.server.domain.card.entity.CardTransaction
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 카드 거래 내역 응답 DTO
 *
 * 파싱에 성공한 거래 내역을 반환합니다.
 * 파싱 실패 내역은 CardParseFailureResponse를 사용합니다.
 */
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
    @Schema(description = "카드 끝 4자리", example = "6518")
    val cardLastFourDigits: String?,
    @Schema(description = "결제 금액 (원)", example = "4500")
    val amount: BigDecimal,
    @Schema(description = "거래 일시", example = "2025-01-15T17:52:00")
    val transactionDate: LocalDateTime,
    @Schema(description = "사용처 (가맹점명)", example = "버거킹건대입구역")
    val merchantName: String,
    @Schema(description = "누적 사용 금액 (원)", example = "773774")
    val accumulatedAmount: BigDecimal?,
    @Schema(description = "생성 일시", example = "2025-01-15T17:52:30")
    val createdAt: LocalDateTime,
    @Schema(description = "취소 여부")
    val cancelled: Boolean = false,
    @Schema(description = "주요 태그 (MAIN, 최대 1개)")
    val mainTag: TransactionTagResponse? = null,
    @Schema(description = "세부 태그 목록 (DETAIL)")
    val detailTags: List<TransactionTagResponse> = emptyList(),
    @Schema(description = "전체 태그 목록 (하위 호환)")
    val tags: List<TransactionTagResponse> = emptyList()
) {
    companion object {
        /**
         * Entity를 Response DTO로 변환
         *
         * @param entity CardTransaction 엔티티
         * @param tags 태그 목록 (기본값: 빈 리스트)
         * @return CardTransactionResponse DTO
         */
        fun from(entity: CardTransaction, tags: List<TransactionTagResponse> = emptyList()): CardTransactionResponse {
            val mainTag = tags.find { it.tagType == com.woo.server.common.enums.TagType.MAIN }
            val detailTags = tags.filter { it.tagType == com.woo.server.common.enums.TagType.DETAIL }
            return CardTransactionResponse(
                id = entity.id!!,
                phoneNumber = entity.phoneNumber,
                cardCompany = entity.cardCompany,
                cardCompanyName = entity.cardCompany.displayName,
                cardLastFourDigits = entity.cardLastFourDigits,
                amount = entity.amount,
                transactionDate = entity.transactionDate,
                merchantName = entity.merchantName,
                accumulatedAmount = entity.accumulatedAmount,
                createdAt = entity.createdAt,
                cancelled = entity.cancelled,
                mainTag = mainTag,
                detailTags = detailTags,
                tags = tags
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
    @Schema(description = "저장된 거래 정보 (파싱 성공 시)")
    val transaction: CardTransactionResponse?,
    @Schema(description = "저장된 파싱 실패 정보 (파싱 실패 시)")
    val parseFailure: CardParseFailureResponse?,
    @Schema(description = "저장된 청구서 정보 (청구서 처리 시)")
    val bill: CardMonthlyBillResponse? = null
) {
    companion object {
        /**
         * 성공 응답 생성
         */
        fun success(transaction: CardTransactionResponse): CardMessageProcessResponse {
            return CardMessageProcessResponse(
                success = true,
                message = "문자 메시지가 성공적으로 처리되었습니다",
                transaction = transaction,
                parseFailure = null
            )
        }

        /**
         * 실패 응답 생성 (파싱 실패, 별도 테이블에 저장됨)
         */
        fun parseFailed(failure: CardParseFailureResponse): CardMessageProcessResponse {
            return CardMessageProcessResponse(
                success = false,
                message = "문자 파싱에 실패했지만 원본 메시지가 저장되었습니다",
                transaction = null,
                parseFailure = failure
            )
        }

        /**
         * 취소 처리 성공 응답 생성 (원본 거래 취소 표시)
         */
        fun cancelSuccess(transaction: CardTransactionResponse): CardMessageProcessResponse {
            return CardMessageProcessResponse(
                success = true,
                message = "결제 취소가 성공적으로 처리되었습니다",
                transaction = transaction,
                parseFailure = null
            )
        }

        /**
         * 취소 처리 응답 생성 (원본 미매칭, 취소 거래 별도 저장)
         */
        fun cancelUnmatched(transaction: CardTransactionResponse): CardMessageProcessResponse {
            return CardMessageProcessResponse(
                success = true,
                message = "원본 거래를 찾을 수 없어 취소 거래가 별도 저장되었습니다",
                transaction = transaction,
                parseFailure = null
            )
        }

        /**
         * 중복 거래 응답 생성
         */
        fun duplicate(): CardMessageProcessResponse {
            return CardMessageProcessResponse(
                success = false,
                message = "이미 동일한 거래가 존재합니다",
                transaction = null,
                parseFailure = null
            )
        }

        /**
         * 오류 응답 생성
         */
        fun error(errorMessage: String): CardMessageProcessResponse {
            return CardMessageProcessResponse(
                success = false,
                message = errorMessage,
                transaction = null,
                parseFailure = null
            )
        }

        /**
         * 청구서 처리 결과를 거래 처리 응답으로 변환
         */
        fun billProcessed(billResult: CardMonthlyBillProcessResponse): CardMessageProcessResponse {
            return CardMessageProcessResponse(
                success = billResult.success,
                message = billResult.message,
                transaction = null,
                parseFailure = null,
                bill = billResult.bill
            )
        }
    }
}
