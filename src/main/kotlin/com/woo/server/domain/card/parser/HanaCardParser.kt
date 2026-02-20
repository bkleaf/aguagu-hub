package com.woo.server.domain.card.parser

import com.woo.server.common.enums.CardCompany
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.Year

/**
 * 하나카드 문자 메시지 파서
 *
 * 하나카드의 결제 승인/취소 문자를 파싱하여 거래 정보를 추출합니다.
 *
 * 지원하는 문자 형식:
 *
 * 승인 형식:
 * ```
 * 금액 222,200원
 * 카드 MG+ 하나7*2*
 * 손님명 전*우
 * 거래종류 신용
 * 거래구분 일시불
 * 사용처 TNF광진이스트폴
 * 거래시간 01/30 10:59
 * 누적금액 1,135,523원
 * ```
 *
 * 한 줄 승인 형식:
 * ```
 * MG+ 하나7*2*승인 전*우 11,200원 일시불 02/02 15:30 호랑마켓광장점 누적1,160,487원
 * ```
 *
 * 취소 형식:
 * ```
 * [하나카드] 하나 7*2* 취소 전*우 14,379원 1/28 인터넷상거래_TOSS
 * ```
 */
@Component
class HanaCardParser : CardMessageParser {

    companion object {
        /** 하나카드 문자 식별 키워드 */
        private val HANA_KEYWORDS = listOf("하나", "HANA")

        // === 승인 형식 정규식 ===

        /** 금액 추출 (예: "금액 222,200원") */
        private val APPROVAL_AMOUNT_PATTERN = Regex("""금액\s+([0-9,]+)원""")

        /** 카드 끝자리 추출 (예: "하나7*2*" 또는 "하나 7*2*") */
        private val CARD_NUMBER_PATTERN = Regex("""하나\s?(\d)\*(\d)\*""")

        /** 사용처 추출 (예: "사용처 TNF광진이스트폴") */
        private val MERCHANT_PATTERN = Regex("""사용처\s+(.+)""")

        /** 거래시간 추출 (예: "거래시간 01/30 10:59") */
        private val APPROVAL_DATETIME_PATTERN = Regex("""거래시간\s+(\d{1,2})/(\d{1,2})\s+(\d{1,2}):(\d{2})""")

        /** 누적금액 추출 (예: "누적금액 1,135,523원") */
        private val ACCUMULATED_PATTERN = Regex("""누적금액\s+([0-9,]+)원""")

        // === 한 줄 승인 형식 정규식 ===

        /** 한 줄 승인 패턴 (예: "하나7*2*승인 전*우 11,200원 일시불 02/02 15:30 호랑마켓광장점 누적1,160,487원") */
        private val COMPACT_APPROVAL_PATTERN = Regex(
            """하나\s?(\d)\*(\d)\*승인\s+\S+\s+([0-9,]+)원\s+\S+\s+(\d{1,2})/(\d{1,2})\s+(\d{1,2}):(\d{2})\s+(.+?)\s*누적([0-9,]+)원"""
        )

        // === 취소 형식 정규식 ===

        /** 취소 금액 추출 (예: "14,379원") */
        private val CANCEL_AMOUNT_PATTERN = Regex("""취소\s+\S+\s+([0-9,]+)원""")

        /** 취소 날짜 추출 (예: "1/28") */
        private val CANCEL_DATE_PATTERN = Regex("""([0-9,]+)원\s+(\d{1,2})/(\d{1,2})""")

        /** 취소 사용처 추출 (날짜 이후 텍스트) */
        private val CANCEL_MERCHANT_PATTERN = Regex("""\d{1,2}/\d{1,2}\s+(.+)""")
    }

    /**
     * 하나카드를 지원 카드사로 반환
     */
    override fun getSupportedCardCompany(): CardCompany = CardCompany.HANA

    /**
     * 메시지가 하나카드 문자인지 확인
     *
     * @param message 문자 메시지
     * @return 하나카드 문자 여부 (승인 또는 취소)
     */
    override fun canParse(message: String): Boolean {
        val hasKeyword = HANA_KEYWORDS.any { message.contains(it, ignoreCase = true) }
        val isMultiLineApproval = message.contains("금액") && message.contains("사용처")
        val isCompactApproval = COMPACT_APPROVAL_PATTERN.containsMatchIn(message)
        val isCancel = message.contains("취소")
        return hasKeyword && (isMultiLineApproval || isCompactApproval || isCancel)
    }

    /**
     * 하나카드 문자 메시지를 파싱하여 거래 정보 추출
     *
     * @param message 하나카드 결제 문자
     * @return 파싱 결과
     */
    override fun parse(message: String): ParseResult {
        return try {
            if (message.contains("취소")) {
                parseCancel(message)
            } else if (COMPACT_APPROVAL_PATTERN.containsMatchIn(message)) {
                parseCompactApproval(message)
            } else {
                parseApproval(message)
            }
        } catch (e: Exception) {
            ParseResult.failure("파싱 중 오류 발생: ${e.message}")
        }
    }

    /**
     * 승인 문자 파싱
     *
     * 키-값 형태의 여러 줄 문자에서 거래 정보를 추출한다.
     */
    private fun parseApproval(message: String): ParseResult {
        val amount = APPROVAL_AMOUNT_PATTERN.find(message)?.let {
            parseDecimal(it.groupValues[1])
        } ?: return ParseResult.failure("금액을 추출할 수 없습니다")

        val transactionDate = APPROVAL_DATETIME_PATTERN.find(message)?.let {
            buildDateTime(
                month = it.groupValues[1].toInt(),
                day = it.groupValues[2].toInt(),
                hour = it.groupValues[3].toInt(),
                minute = it.groupValues[4].toInt()
            )
        } ?: return ParseResult.failure("거래 일시를 추출할 수 없습니다")

        val merchantName = MERCHANT_PATTERN.find(message)?.groupValues?.get(1)?.trim()
            ?: return ParseResult.failure("사용처를 추출할 수 없습니다")

        val accumulatedAmount = ACCUMULATED_PATTERN.find(message)?.let {
            parseDecimal(it.groupValues[1])
        }

        val cardLastFourDigits = extractCardLastFourDigits(message)

        return ParseResult.success(
            amount = amount,
            transactionDate = transactionDate,
            merchantName = merchantName,
            accumulatedAmount = accumulatedAmount,
            cardLastFourDigits = cardLastFourDigits
        )
    }

    /**
     * 한 줄 승인 문자 파싱
     *
     * "MG+ 하나7*2*승인 전*우 11,200원 일시불 02/02 15:30 호랑마켓광장점 누적1,160,487원" 형태를 파싱한다.
     */
    private fun parseCompactApproval(message: String): ParseResult {
        val match = COMPACT_APPROVAL_PATTERN.find(message)
            ?: return ParseResult.failure("한 줄 승인 형식을 파싱할 수 없습니다")

        val cardDigit1 = match.groupValues[1]
        val cardDigit2 = match.groupValues[2]
        val amount = parseDecimal(match.groupValues[3])
            ?: return ParseResult.failure("금액을 추출할 수 없습니다")
        val month = match.groupValues[4].toInt()
        val day = match.groupValues[5].toInt()
        val hour = match.groupValues[6].toInt()
        val minute = match.groupValues[7].toInt()
        val merchantName = match.groupValues[8].trim()
        val accumulatedAmount = parseDecimal(match.groupValues[9])

        val transactionDate = buildDateTime(month, day, hour, minute)
        val cardLastFourDigits = "${cardDigit1}*${cardDigit2}*"

        return ParseResult.success(
            amount = amount,
            transactionDate = transactionDate,
            merchantName = merchantName,
            accumulatedAmount = accumulatedAmount,
            cardLastFourDigits = cardLastFourDigits
        )
    }

    /**
     * 취소 문자 파싱
     *
     * 한 줄 형태의 취소 문자에서 거래 정보를 추출한다.
     * 취소 문자에는 시간 정보가 없으므로 00:00으로 설정한다.
     */
    private fun parseCancel(message: String): ParseResult {
        val amount = CANCEL_AMOUNT_PATTERN.find(message)?.let {
            parseDecimal(it.groupValues[1])
        } ?: return ParseResult.failure("금액을 추출할 수 없습니다")

        val transactionDate = CANCEL_DATE_PATTERN.find(message)?.let {
            buildDateTime(
                month = it.groupValues[2].toInt(),
                day = it.groupValues[3].toInt(),
                hour = 0,
                minute = 0
            )
        } ?: return ParseResult.failure("거래 일시를 추출할 수 없습니다")

        val merchantName = CANCEL_MERCHANT_PATTERN.find(message)?.groupValues?.get(1)?.trim()
            ?: return ParseResult.failure("사용처를 추출할 수 없습니다")

        val cardLastFourDigits = extractCardLastFourDigits(message)

        return ParseResult.success(
            amount = amount,
            transactionDate = transactionDate,
            merchantName = "[취소] $merchantName",
            cardLastFourDigits = cardLastFourDigits
        )
    }

    /**
     * 카드 끝자리 추출 (마스킹된 형태에서 보이는 숫자 조합)
     */
    private fun extractCardLastFourDigits(message: String): String? {
        return CARD_NUMBER_PATTERN.find(message)?.let {
            "${it.groupValues[1]}*${it.groupValues[2]}*"
        }
    }

    /**
     * 콤마가 포함된 금액 문자열을 BigDecimal로 변환
     */
    private fun parseDecimal(value: String): BigDecimal? {
        return try {
            BigDecimal(value.replace(",", ""))
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * 연도 없는 날짜/시간 정보로 LocalDateTime 생성
     *
     * 현재 연도를 사용하며, 미래 월인 경우에만 작년으로 처리한다.
     * (같은 날 몇 분 차이로 작년으로 밀리는 문제 방지를 위해 날짜 단위로 비교)
     */
    private fun buildDateTime(month: Int, day: Int, hour: Int, minute: Int): LocalDateTime {
        val today = java.time.LocalDate.now()
        var year = today.year
        val date = java.time.LocalDate.of(year, month, day)
        if (date.isAfter(today)) {
            year -= 1
        }
        return LocalDateTime.of(year, month, day, hour, minute)
    }
}
