package com.woo.server.domain.card.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 태그별 금액 요약 DTO
 *
 * 특정 태그의 총 사용 금액, 거래 건수, 평균 금액, 비율 정보를 담습니다.
 */
@Schema(description = "태그별 금액 요약")
data class TagAmountSummary(
    @Schema(description = "태그 ID", example = "1")
    val tagId: Long,
    @Schema(description = "태그 이름", example = "식비")
    val tagName: String,
    @Schema(description = "태그 색상 (hex)", example = "#4CAF50")
    val tagColor: String,
    @Schema(description = "총 사용 금액", example = "150000")
    val totalAmount: BigDecimal,
    @Schema(description = "거래 건수", example = "12")
    val transactionCount: Long,
    @Schema(description = "평균 거래 금액", example = "12500")
    val averageAmount: BigDecimal,
    @Schema(description = "전체 대비 비율 (%)", example = "25.5")
    val percentage: Double
)

/**
 * 태그 통계 요약 응답 DTO
 *
 * 기간 내 태그별 금액 요약과 전체 합산 정보를 포함합니다.
 * 하나의 거래에 여러 태그가 부여된 경우 각 태그 합계에 모두 포함됩니다.
 */
@Schema(description = "태그 통계 요약 응답")
data class TagStatisticsSummaryResponse(
    @Schema(description = "태그별 금액 요약 목록")
    val tags: List<TagAmountSummary>,
    @Schema(description = "태그 지정 거래 총액 (태그별 합산, 중복 포함)", example = "600000")
    val totalAmount: BigDecimal,
    @Schema(description = "미분류 거래 총액", example = "50000")
    val untaggedAmount: BigDecimal,
    @Schema(description = "조회 시작일", example = "2026-01-01")
    val startDate: LocalDate,
    @Schema(description = "조회 종료일", example = "2026-01-31")
    val endDate: LocalDate
)

/**
 * 태그별 월간 시리즈 DTO
 *
 * 하나의 태그에 대한 월별 사용 금액 데이터를 담습니다. (Bar 차트용)
 */
@Schema(description = "태그별 월간 시리즈")
data class TagMonthlySeries(
    @Schema(description = "태그 ID", example = "1")
    val tagId: Long,
    @Schema(description = "태그 이름", example = "식비")
    val tagName: String,
    @Schema(description = "태그 색상 (hex)", example = "#4CAF50")
    val tagColor: String,
    @Schema(description = "월별 금액 데이터 (months 순서에 대응)")
    val data: List<BigDecimal>
)

/**
 * 태그별 월간 추이 응답 DTO
 *
 * Stacked Bar 차트에 사용할 월간 추이 데이터를 담습니다.
 */
@Schema(description = "태그별 월간 추이 응답")
data class TagMonthlyTrendResponse(
    @Schema(description = "월 목록 (yyyy-MM 형식)", example = "[\"2026-01\", \"2026-02\"]")
    val months: List<String>,
    @Schema(description = "태그별 시리즈 데이터")
    val series: List<TagMonthlySeries>,
    @Schema(description = "조회 시작일", example = "2025-09-01")
    val startDate: LocalDate,
    @Schema(description = "조회 종료일", example = "2026-02-28")
    val endDate: LocalDate
)

/**
 * 태그 금액 합산 응답 DTO
 *
 * 선택한 태그들의 합산 금액, 태그별 상세, 월간 추이를 포함합니다.
 */
@Schema(description = "태그 금액 합산 응답")
data class TagAmountAggregateResponse(
    @Schema(description = "태그별 금액 요약 목록")
    val tags: List<TagAmountSummary>,
    @Schema(description = "선택 태그 총 합산 금액", example = "850000")
    val grandTotal: BigDecimal,
    @Schema(description = "월간 추이 데이터")
    val monthlyTrend: TagMonthlyTrendResponse,
    @Schema(description = "조회 시작일", example = "2026-01-01")
    val startDate: LocalDate,
    @Schema(description = "조회 종료일", example = "2026-02-28")
    val endDate: LocalDate
)

/**
 * 미분류 거래 요약 응답 DTO
 *
 * 태그가 부여되지 않은 거래의 합산 정보를 담습니다.
 */
@Schema(description = "미분류 거래 요약 응답")
data class UntaggedTransactionSummaryResponse(
    @Schema(description = "미분류 거래 총액", example = "50000")
    val totalAmount: BigDecimal,
    @Schema(description = "미분류 거래 건수", example = "5")
    val transactionCount: Long,
    @Schema(description = "조회 시작일", example = "2026-01-01")
    val startDate: LocalDate,
    @Schema(description = "조회 종료일", example = "2026-01-31")
    val endDate: LocalDate
)
