package com.woo.server.domain.card.service

import com.woo.server.common.enums.TagType
import com.woo.server.domain.card.dto.*
import com.woo.server.domain.card.repository.CardTransactionRepository
import com.woo.server.domain.card.repository.TransactionTagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 태그 통계 서비스
 *
 * 태그별 소비 금액 집계, 월간 추이, 미분류 거래 통계를 제공합니다.
 * 하나의 거래에 복수 태그가 부여된 경우 각 태그 합계에 모두 포함됩니다.
 * tagType 필터로 주요(MAIN)/세부(DETAIL) 태그별 통계를 조회할 수 있습니다.
 */
@Service
@Transactional(readOnly = true)
class TagStatisticsService(
    private val transactionTagRepository: TransactionTagRepository,
    private val cardTransactionRepository: CardTransactionRepository
) {

    /**
     * 태그별 금액 요약을 조회합니다.
     *
     * 기간 내 태그별 총 사용 금액, 거래 건수, 평균 금액, 비율을 집계합니다.
     * 미분류 거래 총액도 함께 반환합니다.
     *
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @param tagType 태그 유형 필터 (null이면 전체)
     * @return 태그 통계 요약 응답
     */
    fun getSummary(startDate: LocalDate, endDate: LocalDate, tagType: TagType? = null): TagStatisticsSummaryResponse {
        val startDateTime = startDate.atStartOfDay()
        val endDateTime = endDate.atTime(23, 59, 59)

        // 태그별 집계 조회 (태그 유형 필터 적용)
        val tagAggregations = if (tagType != null) {
            transactionTagRepository.sumAmountByTagAndPeriodAndType(startDateTime, endDateTime, tagType)
        } else {
            transactionTagRepository.sumAmountByTagAndPeriod(startDateTime, endDateTime)
        }

        // 전체 태그 금액 합산 (비율 계산용)
        val tagTotalAmount = tagAggregations.fold(BigDecimal.ZERO) { acc, row ->
            acc + (row[3] as BigDecimal)
        }

        // 미분류 거래 집계
        val untaggedAmount = cardTransactionRepository.sumUntaggedAmountByPeriod(startDateTime, endDateTime)

        // 비율 계산 기준: 태그 합산 + 미분류
        val grandTotal = tagTotalAmount + untaggedAmount

        val tags = tagAggregations.map { row ->
            val totalAmount = row[3] as BigDecimal
            val transactionCount = (row[4] as Number).toLong()
            val averageAmount = if (transactionCount > 0) {
                totalAmount.divide(BigDecimal(transactionCount), 0, RoundingMode.HALF_UP)
            } else BigDecimal.ZERO
            val percentage = if (grandTotal > BigDecimal.ZERO) {
                totalAmount.multiply(BigDecimal(100))
                    .divide(grandTotal, 1, RoundingMode.HALF_UP)
                    .toDouble()
            } else 0.0

            TagAmountSummary(
                tagId = (row[0] as Number).toLong(),
                tagName = row[1] as String,
                tagColor = row[2] as String,
                totalAmount = totalAmount,
                transactionCount = transactionCount,
                averageAmount = averageAmount,
                percentage = percentage
            )
        }

        return TagStatisticsSummaryResponse(
            tags = tags,
            totalAmount = tagTotalAmount,
            untaggedAmount = untaggedAmount,
            startDate = startDate,
            endDate = endDate
        )
    }

    /**
     * 태그별 월간 추이를 조회합니다.
     *
     * 기간 내 각 태그의 월별 사용 금액을 집계하여 Stacked Bar 차트 데이터로 반환합니다.
     *
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @param tagType 태그 유형 필터 (null이면 전체)
     * @return 태그별 월간 추이 응답
     */
    fun getMonthlyTrend(startDate: LocalDate, endDate: LocalDate, tagType: TagType? = null): TagMonthlyTrendResponse {
        val startDateTime = startDate.atStartOfDay()
        val endDateTime = endDate.atTime(23, 59, 59)

        // 월 목록 생성
        val months = generateMonthList(startDate, endDate)

        // 태그별·월별 집계 조회 (태그 유형 필터 적용)
        val rawData = if (tagType != null) {
            transactionTagRepository.sumAmountByTagAndMonthAndType(startDateTime, endDateTime, tagType.name)
        } else {
            transactionTagRepository.sumAmountByTagAndMonth(startDateTime, endDateTime)
        }

        // 태그별로 그룹화
        val tagMonthMap = mutableMapOf<Long, MutableMap<String, BigDecimal>>()
        val tagInfoMap = mutableMapOf<Long, Pair<String, String>>() // tagId -> (name, color)

        rawData.forEach { row ->
            val tagId = (row[0] as Number).toLong()
            val tagName = row[1] as String
            val tagColor = row[2] as String
            val yearMonth = row[3] as String
            val totalAmount = row[4] as BigDecimal

            tagInfoMap[tagId] = Pair(tagName, tagColor)
            tagMonthMap.getOrPut(tagId) { mutableMapOf() }[yearMonth] = totalAmount
        }

        // 시리즈 데이터 구성 (각 태그의 월별 금액, 없는 월은 0)
        val series = tagInfoMap.map { (tagId, info) ->
            val monthData = tagMonthMap[tagId] ?: emptyMap()
            TagMonthlySeries(
                tagId = tagId,
                tagName = info.first,
                tagColor = info.second,
                data = months.map { month -> monthData[month] ?: BigDecimal.ZERO }
            )
        }

        return TagMonthlyTrendResponse(
            months = months,
            series = series,
            startDate = startDate,
            endDate = endDate
        )
    }

    /**
     * 선택한 태그 ID 목록에 대한 합산 금액 + 월간 추이를 조회합니다.
     *
     * MAIN 태그 기준으로 선택 태그별 금액 집계 및 월간 추이를 반환합니다.
     *
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @param tagIds 선택한 태그 ID 목록
     * @return 태그 금액 합산 응답
     */
    fun getAggregateByTagIds(startDate: LocalDate, endDate: LocalDate, tagIds: List<Long>): TagAmountAggregateResponse {
        val startDateTime = startDate.atStartOfDay()
        val endDateTime = endDate.atTime(23, 59, 59)

        // 선택 태그별 금액 집계 (MAIN 태그 기준)
        val tagAggregations = transactionTagRepository.sumAmountByTagIdsAndPeriod(
            startDateTime, endDateTime, tagIds, TagType.MAIN
        )

        // 전체 합산 금액
        val grandTotal = tagAggregations.fold(BigDecimal.ZERO) { acc, row ->
            acc + (row[3] as BigDecimal)
        }

        // 태그별 요약 구성
        val tags = tagAggregations.map { row ->
            val totalAmount = row[3] as BigDecimal
            val transactionCount = (row[4] as Number).toLong()
            val averageAmount = if (transactionCount > 0) {
                totalAmount.divide(BigDecimal(transactionCount), 0, RoundingMode.HALF_UP)
            } else BigDecimal.ZERO
            val percentage = if (grandTotal > BigDecimal.ZERO) {
                totalAmount.multiply(BigDecimal(100))
                    .divide(grandTotal, 1, RoundingMode.HALF_UP)
                    .toDouble()
            } else 0.0

            TagAmountSummary(
                tagId = (row[0] as Number).toLong(),
                tagName = row[1] as String,
                tagColor = row[2] as String,
                totalAmount = totalAmount,
                transactionCount = transactionCount,
                averageAmount = averageAmount,
                percentage = percentage
            )
        }

        // 월간 추이 조회
        val months = generateMonthList(startDate, endDate)
        val rawTrend = transactionTagRepository.sumAmountByTagIdsAndMonth(
            startDateTime, endDateTime, tagIds, TagType.MAIN.name
        )

        val tagMonthMap = mutableMapOf<Long, MutableMap<String, BigDecimal>>()
        val tagInfoMap = mutableMapOf<Long, Pair<String, String>>()

        rawTrend.forEach { row ->
            val tagId = (row[0] as Number).toLong()
            val tagName = row[1] as String
            val tagColor = row[2] as String
            val yearMonth = row[3] as String
            val totalAmount = row[4] as BigDecimal

            tagInfoMap[tagId] = Pair(tagName, tagColor)
            tagMonthMap.getOrPut(tagId) { mutableMapOf() }[yearMonth] = totalAmount
        }

        val series = tagInfoMap.map { (tagId, info) ->
            val monthData = tagMonthMap[tagId] ?: emptyMap()
            TagMonthlySeries(
                tagId = tagId,
                tagName = info.first,
                tagColor = info.second,
                data = months.map { month -> monthData[month] ?: BigDecimal.ZERO }
            )
        }

        val monthlyTrend = TagMonthlyTrendResponse(
            months = months,
            series = series,
            startDate = startDate,
            endDate = endDate
        )

        return TagAmountAggregateResponse(
            tags = tags,
            grandTotal = grandTotal,
            monthlyTrend = monthlyTrend,
            startDate = startDate,
            endDate = endDate
        )
    }

    /**
     * 미분류 거래 요약을 조회합니다.
     *
     * 태그가 부여되지 않은 거래의 합산 금액과 건수를 반환합니다.
     *
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @return 미분류 거래 요약 응답
     */
    fun getUntaggedSummary(startDate: LocalDate, endDate: LocalDate): UntaggedTransactionSummaryResponse {
        val startDateTime = startDate.atStartOfDay()
        val endDateTime = endDate.atTime(23, 59, 59)

        val totalAmount = cardTransactionRepository.sumUntaggedAmountByPeriod(startDateTime, endDateTime)
        val transactionCount = cardTransactionRepository.countUntaggedByPeriod(startDateTime, endDateTime)

        return UntaggedTransactionSummaryResponse(
            totalAmount = totalAmount,
            transactionCount = transactionCount,
            startDate = startDate,
            endDate = endDate
        )
    }

    /**
     * 시작일과 종료일 사이의 yyyy-MM 형식 월 목록을 생성합니다.
     */
    private fun generateMonthList(startDate: LocalDate, endDate: LocalDate): List<String> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
        val months = mutableListOf<String>()
        var current = YearMonth.from(startDate)
        val end = YearMonth.from(endDate)

        while (!current.isAfter(end)) {
            months.add(current.format(formatter))
            current = current.plusMonths(1)
        }
        return months
    }
}
