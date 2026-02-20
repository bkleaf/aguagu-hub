package com.woo.server.domain.card.dto

import com.woo.server.common.enums.CardCompany
import com.woo.server.common.enums.LimitType
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 개별 카드 사용 현황 응답 DTO
 */
@Schema(description = "카드별 사용 현황 응답")
data class CardUsageSummaryResponse(
    @Schema(description = "신용카드 ID", example = "SAMSUNG_4300")
    val creditCardId: String,

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

    @Schema(description = "정산기간 시작 날짜", example = "2026-02-01")
    val periodStart: LocalDate,

    @Schema(description = "정산기간 종료 날짜", example = "2026-02-28")
    val periodEnd: LocalDate,

    @Schema(description = "정산기간 거래 합산 사용액")
    val usedAmount: BigDecimal,

    @Schema(description = "한도 설정 여부")
    val hasLimit: Boolean,

    @Schema(description = "한도 유형", nullable = true)
    val limitType: LimitType? = null,

    @Schema(description = "한도 유형 이름", nullable = true, example = "월간")
    val limitTypeName: String? = null,

    @Schema(description = "한도 금액", nullable = true)
    val limitAmount: BigDecimal? = null,

    @Schema(description = "한도 기간 내 사용액", nullable = true)
    val limitUsedAmount: BigDecimal? = null,

    @Schema(description = "잔여 한도 (한도 기간 기준)", nullable = true)
    val remaining: BigDecimal? = null,

    @Schema(description = "사용률 (0~100, 한도 기간 기준)", nullable = true, example = "45.5")
    val usagePercent: Double? = null
)

/**
 * 전체 카드 사용 현황 요약 응답 DTO
 */
@Schema(description = "전체 카드 사용 현황 요약")
data class UsageSummaryOverview(
    @Schema(description = "카드별 사용 현황 목록")
    val cards: List<CardUsageSummaryResponse>,

    @Schema(description = "전체 사용액 합계")
    val totalUsedAmount: BigDecimal
)
