package com.woo.server.domain.card.controller

import com.woo.server.common.enums.TagType
import com.woo.server.domain.card.dto.TagAmountAggregateResponse
import com.woo.server.domain.card.dto.TagMonthlyTrendResponse
import com.woo.server.domain.card.dto.TagStatisticsSummaryResponse
import com.woo.server.domain.card.dto.UntaggedTransactionSummaryResponse
import com.woo.server.domain.card.service.TagStatisticsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

/**
 * 태그 통계 컨트롤러
 *
 * 태그별 소비 집계, 월간 추이, 미분류 거래 통계 API를 제공합니다.
 * tagType 파라미터로 주요(MAIN)/세부(DETAIL) 태그별 통계를 필터링할 수 있습니다.
 */
@Tag(name = "태그 통계", description = "태그별 소비 통계 및 차트 데이터 API")
@RestController
@RequestMapping("/api/v1/card/tags/statistics")
class TagStatisticsController(
    private val tagStatisticsService: TagStatisticsService
) {

    /**
     * 태그별 금액 요약을 조회합니다.
     *
     * 기간 내 각 태그의 총 사용 금액, 거래 건수, 평균, 비율을 반환합니다.
     * 복수 태그 거래는 각 태그에 중복 포함됩니다.
     * tagType 파라미터로 MAIN/DETAIL 필터링이 가능합니다.
     */
    @Operation(summary = "태그별 금액 요약", description = "기간 내 태그별 합계/건수/평균/비율을 조회합니다. tagType으로 MAIN/DETAIL 필터 가능.")
    @GetMapping("/summary")
    fun getSummary(
        @Parameter(description = "조회 시작일 (yyyy-MM-dd)", example = "2026-01-01")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @Parameter(description = "조회 종료일 (yyyy-MM-dd)", example = "2026-01-31")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @Parameter(description = "태그 유형 필터 (MAIN/DETAIL, 미지정 시 전체)", example = "MAIN")
        @RequestParam(required = false) tagType: TagType? = null
    ): ResponseEntity<TagStatisticsSummaryResponse> {
        return ResponseEntity.ok(tagStatisticsService.getSummary(startDate, endDate, tagType))
    }

    /**
     * 태그별 월간 추이를 조회합니다.
     *
     * 기간 내 각 태그의 월별 사용 금액 데이터를 Stacked Bar 차트 형식으로 반환합니다.
     * tagType 파라미터로 MAIN/DETAIL 필터링이 가능합니다.
     */
    @Operation(summary = "태그별 월간 추이", description = "태그별 월간 사용 금액 추이를 조회합니다 (Bar 차트용). tagType으로 MAIN/DETAIL 필터 가능.")
    @GetMapping("/monthly-trend")
    fun getMonthlyTrend(
        @Parameter(description = "조회 시작일 (yyyy-MM-dd)", example = "2025-09-01")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @Parameter(description = "조회 종료일 (yyyy-MM-dd)", example = "2026-02-28")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @Parameter(description = "태그 유형 필터 (MAIN/DETAIL, 미지정 시 전체)", example = "MAIN")
        @RequestParam(required = false) tagType: TagType? = null
    ): ResponseEntity<TagMonthlyTrendResponse> {
        return ResponseEntity.ok(tagStatisticsService.getMonthlyTrend(startDate, endDate, tagType))
    }

    /**
     * 선택 태그 금액 합산을 조회합니다.
     *
     * 선택한 태그 ID 목록에 대한 합산 금액, 태그별 상세, 월간 추이를 반환합니다.
     * MAIN 태그 기준으로 집계됩니다.
     */
    @Operation(summary = "선택 태그 금액 합산", description = "선택한 태그 ID들의 합산 금액 + 월간 추이를 조회합니다 (MAIN 태그 기준).")
    @GetMapping("/aggregate")
    fun getAggregate(
        @Parameter(description = "조회 시작일 (yyyy-MM-dd)", example = "2026-01-01")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @Parameter(description = "조회 종료일 (yyyy-MM-dd)", example = "2026-02-28")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @Parameter(description = "태그 ID 목록 (콤마 구분)", example = "1,2,3")
        @RequestParam tagIds: List<Long>
    ): ResponseEntity<TagAmountAggregateResponse> {
        return ResponseEntity.ok(tagStatisticsService.getAggregateByTagIds(startDate, endDate, tagIds))
    }

    /**
     * 미분류 거래 요약을 조회합니다.
     *
     * 태그가 부여되지 않은 거래의 합산 금액과 건수를 반환합니다.
     */
    @Operation(summary = "미분류 거래 요약", description = "태그 미부여 거래의 총액과 건수를 조회합니다.")
    @GetMapping("/untagged")
    fun getUntaggedSummary(
        @Parameter(description = "조회 시작일 (yyyy-MM-dd)", example = "2026-01-01")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @Parameter(description = "조회 종료일 (yyyy-MM-dd)", example = "2026-01-31")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<UntaggedTransactionSummaryResponse> {
        return ResponseEntity.ok(tagStatisticsService.getUntaggedSummary(startDate, endDate))
    }
}
