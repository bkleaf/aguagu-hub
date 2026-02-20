package com.woo.server.domain.card.dto

import com.woo.server.common.enums.CardCompany
import com.woo.server.domain.card.entity.CardMonthlyBill
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 월별 청구서 응답 DTO
 */
@Schema(description = "월별 청구서 응답")
data class CardMonthlyBillResponse(
    @Schema(description = "청구서 ID", example = "1")
    val id: Long,
    @Schema(description = "발신자 전화번호", example = "15888700")
    val phoneNumber: String?,
    @Schema(description = "카드사 코드", example = "SAMSUNG")
    val cardCompany: CardCompany,
    @Schema(description = "카드사 이름", example = "삼성카드")
    val cardCompanyName: String,
    @Schema(description = "결제일", example = "2026-02-25")
    val billingDate: LocalDate,
    @Schema(description = "청구 금액 (원)", example = "150000")
    val billingAmount: BigDecimal,
    @Schema(description = "기준일", example = "2026-02-10")
    val referenceDate: LocalDate,
    @Schema(description = "생성 일시", example = "2026-02-12T10:30:00")
    val createdAt: LocalDateTime
) {
    companion object {
        /**
         * Entity를 Response DTO로 변환
         *
         * @param entity CardMonthlyBill 엔티티
         * @return CardMonthlyBillResponse DTO
         */
        fun from(entity: CardMonthlyBill): CardMonthlyBillResponse {
            return CardMonthlyBillResponse(
                id = entity.id!!,
                phoneNumber = entity.phoneNumber,
                cardCompany = entity.cardCompany,
                cardCompanyName = entity.cardCompany.displayName,
                billingDate = entity.billingDate,
                billingAmount = entity.billingAmount,
                referenceDate = entity.referenceDate,
                createdAt = entity.createdAt
            )
        }
    }
}

/**
 * 월별 청구서 목록 + 합계 응답 DTO
 */
@Schema(description = "월별 청구서 목록 응답 (합계 포함)")
data class CardMonthlyBillListResponse(
    @Schema(description = "청구서 목록")
    val bills: List<CardMonthlyBillResponse>,
    @Schema(description = "청구 합계 금액 (원)", example = "450000")
    val totalAmount: BigDecimal,
    @Schema(description = "조회 년도", example = "2026")
    val year: Int,
    @Schema(description = "조회 월", example = "2")
    val month: Int
)

/**
 * 월별 청구서 처리 결과 응답 DTO
 */
@Schema(description = "월별 청구서 처리 결과 응답")
data class CardMonthlyBillProcessResponse(
    @Schema(description = "처리 성공 여부", example = "true")
    val success: Boolean,
    @Schema(description = "처리 결과 메시지", example = "청구서가 성공적으로 처리되었습니다")
    val message: String,
    @Schema(description = "저장된 청구서 정보 (성공 시)")
    val bill: CardMonthlyBillResponse?
) {
    companion object {
        /**
         * 성공 응답 생성
         */
        fun success(bill: CardMonthlyBillResponse): CardMonthlyBillProcessResponse {
            return CardMonthlyBillProcessResponse(
                success = true,
                message = "청구서가 성공적으로 처리되었습니다",
                bill = bill
            )
        }

        /**
         * 중복 응답 생성
         */
        fun duplicate(): CardMonthlyBillProcessResponse {
            return CardMonthlyBillProcessResponse(
                success = false,
                message = "이미 동일한 청구서가 존재합니다",
                bill = null
            )
        }

        /**
         * 파싱 실패 응답 생성
         */
        fun parseFailed(reason: String): CardMonthlyBillProcessResponse {
            return CardMonthlyBillProcessResponse(
                success = false,
                message = "청구서 파싱 실패: $reason",
                bill = null
            )
        }
    }
}
