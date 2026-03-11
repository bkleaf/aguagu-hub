package com.woo.server.domain.card.parser

import com.woo.server.common.enums.CardCompany
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.Year

/**
 * KB국민카드 문자 메시지 파서
 *
 * KB국민카드의 결제 승인 문자를 파싱하여 거래 정보를 추출합니다.
 * 세 가지 문자 형식을 지원합니다.
 *
 * 형식 1 (상세형):
 * ```
 * KB국민카드4043
 * 승인
 * 28,500 원(일시불)
 * SK매직(주)
 * 고객명 전*우님
 * 승인시간 01/26 10:49
 * 누적 376,785원
 * ```
 *
 * 형식 2 (간략형):
 * ```
 * KB국민카드4043 승인
 * 전*우
 * 36,130원 01/19
 * KT유선상품자동납부
 * ```
 *
 * 형식 3 (중간형):
 * ```
 * KB국민카드4043승인
 * 전*우님
 * 13,200원 일시불
 * 02/03 19:49
 * (주)케이티
 * 누적369,985원
 * ```
 */
@Component
class KbCardParser : CardMessageParser {

    companion object {
        /** KB카드 문자 식별 키워드 */
        private val KB_KEYWORDS = listOf("KB국민카드", "KB국민")

        /** 카드 끝4자리 추출 (예: "KB국민카드4043") */
        private val CARD_LAST_FOUR_PATTERN = Regex("""KB국민카드(\d{4})""")

        // === 형식 1 (상세형) 정규식 ===

        /** 형식 1 금액 추출 (예: "28,500 원(일시불)" 또는 "28,500 원") */
        private val FORMAT1_AMOUNT_PATTERN = Regex("""^\s*([0-9,]+)\s*원""", RegexOption.MULTILINE)

        /** 형식 1 승인시간 추출 (예: "승인시간 01/26 10:49") */
        private val FORMAT1_DATETIME_PATTERN = Regex("""승인시간\s+(\d{1,2})/(\d{1,2})\s+(\d{1,2}):(\d{2})""")

        /** 형식 1 누적금액 추출 (예: "누적 376,785원") */
        private val FORMAT1_ACCUMULATED_PATTERN = Regex("""누적\s+([0-9,]+)원""")

        // === 형식 2 (간략형) 정규식 ===

        /** 형식 2 금액+날짜 추출 (예: "36,130원 01/19") */
        private val FORMAT2_AMOUNT_DATE_PATTERN = Regex("""([0-9,]+)원\s+(\d{1,2})/(\d{1,2})""")

        // === 형식 3 (중간형) 정규식 ===

        /** 형식 3 금액+거래구분 추출 (예: "13,200원 일시불") */
        private val FORMAT3_AMOUNT_PATTERN = Regex("""^([0-9,]+)원\s+일시불\s*$""", RegexOption.MULTILINE)

        /** 형식 3 날짜+시간 추출 (예: "02/03 19:49") - 별도 줄에 위치 */
        private val FORMAT3_DATETIME_PATTERN = Regex("""^(\d{1,2})/(\d{1,2})\s+(\d{1,2}):(\d{2})\s*$""", RegexOption.MULTILINE)

        /** 형식 3 누적금액 추출 (예: "누적369,985원") - 공백 없이 붙어있음 */
        private val FORMAT3_ACCUMULATED_PATTERN = Regex("""누적\s*([0-9,]+)원""")
    }

    /**
     * KB국민카드를 지원 카드사로 반환
     */
    override fun getSupportedCardCompany(): CardCompany = CardCompany.KB

    /**
     * 메시지가 KB국민카드 문자인지 확인
     *
     * @param message 문자 메시지
     * @return KB카드 문자 여부
     */
    override fun canParse(message: String): Boolean {
        return KB_KEYWORDS.any { message.contains(it, ignoreCase = true) }
                && message.contains("승인")
    }

    /**
     * KB국민카드 문자 메시지를 파싱하여 거래 정보 추출
     *
     * 형식 구분 기준:
     * - 형식 1 (상세형): "승인시간" 키워드 존재
     * - 형식 2 (간략형): 금액+날짜가 같은 줄 (예: "36,130원 01/19")
     * - 형식 3 (중간형): 금액+일시불 한 줄, 날짜+시간 별도 줄
     *
     * @param message KB국민카드 결제 승인 문자
     * @return 파싱 결과
     */
    override fun parse(message: String): ParseResult {
        return try {
            when {
                message.contains("승인시간") -> parseFormat1(message)
                FORMAT2_AMOUNT_DATE_PATTERN.containsMatchIn(message) -> parseFormat2(message)
                FORMAT3_AMOUNT_PATTERN.containsMatchIn(message) && FORMAT3_DATETIME_PATTERN.containsMatchIn(message) -> parseFormat3(message)
                else -> ParseResult.failure("지원하지 않는 KB카드 문자 형식입니다")
            }
        } catch (e: Exception) {
            ParseResult.failure("파싱 중 오류 발생: ${e.message}")
        }
    }

    /**
     * 형식 1 (상세형) 파싱
     *
     * 여러 줄에 걸쳐 금액, 사용처, 승인시간, 누적금액이 표시되는 형태.
     * 사용처는 금액 줄 다음 줄에 위치한다.
     */
    private fun parseFormat1(message: String): ParseResult {
        val amount = FORMAT1_AMOUNT_PATTERN.find(message)?.let {
            parseDecimal(it.groupValues[1])
        } ?: return ParseResult.failure("금액을 추출할 수 없습니다")

        val transactionDate = FORMAT1_DATETIME_PATTERN.find(message)?.let {
            buildDateTime(
                month = it.groupValues[1].toInt(),
                day = it.groupValues[2].toInt(),
                hour = it.groupValues[3].toInt(),
                minute = it.groupValues[4].toInt()
            )
        } ?: return ParseResult.failure("거래 일시를 추출할 수 없습니다")

        val merchantName = extractFormat1Merchant(message)
            ?: return ParseResult.failure("사용처를 추출할 수 없습니다")

        val accumulatedAmount = FORMAT1_ACCUMULATED_PATTERN.find(message)?.let {
            parseDecimal(it.groupValues[1])
        }

        val cardLastFourDigits = CARD_LAST_FOUR_PATTERN.find(message)?.groupValues?.get(1)

        return ParseResult.success(
            amount = amount,
            transactionDate = transactionDate,
            merchantName = merchantName,
            accumulatedAmount = accumulatedAmount,
            cardLastFourDigits = cardLastFourDigits
        )
    }

    /**
     * 형식 1에서 사용처 추출
     *
     * 금액이 포함된 줄("원" 포함) 바로 다음 줄이 사용처이다.
     * 단, "고객명", "승인시간", "누적" 등 키워드로 시작하는 줄은 제외한다.
     */
    private fun extractFormat1Merchant(message: String): String? {
        val lines = message.split("\n", "\r\n").map { it.trim() }.filter { it.isNotEmpty() }
        for (i in lines.indices) {
            if (lines[i].contains("원") && FORMAT1_AMOUNT_PATTERN.containsMatchIn(lines[i])) {
                if (i + 1 < lines.size) {
                    val next = lines[i + 1]
                    if (!next.startsWith("고객명") && !next.startsWith("승인시간") && !next.startsWith("누적")) {
                        return next
                    }
                }
            }
        }
        return null
    }

    /**
     * 형식 2 (간략형) 파싱
     *
     * "KB국민카드4043 승인" 으로 시작하며, 금액과 날짜가 같은 줄에 있고,
     * 마지막 줄이 사용처인 형태. 시간 정보가 없으므로 00:00으로 설정한다.
     */
    private fun parseFormat2(message: String): ParseResult {
        val amountDateMatch = FORMAT2_AMOUNT_DATE_PATTERN.find(message)
            ?: return ParseResult.failure("금액/날짜를 추출할 수 없습니다")

        val amount = parseDecimal(amountDateMatch.groupValues[1])
            ?: return ParseResult.failure("금액을 추출할 수 없습니다")

        val transactionDate = buildDateTime(
            month = amountDateMatch.groupValues[2].toInt(),
            day = amountDateMatch.groupValues[3].toInt(),
            hour = 0,
            minute = 0
        )

        val merchantName = extractFormat2Merchant(message)
            ?: return ParseResult.failure("사용처를 추출할 수 없습니다")

        val cardLastFourDigits = CARD_LAST_FOUR_PATTERN.find(message)?.groupValues?.get(1)

        return ParseResult.success(
            amount = amount,
            transactionDate = transactionDate,
            merchantName = merchantName,
            cardLastFourDigits = cardLastFourDigits
        )
    }

    /**
     * 형식 2에서 사용처 추출
     *
     * 금액+날짜 줄 다음 줄이 사용처이다.
     */
    private fun extractFormat2Merchant(message: String): String? {
        val lines = message.split("\n", "\r\n").map { it.trim() }.filter { it.isNotEmpty() }
        for (i in lines.indices) {
            if (FORMAT2_AMOUNT_DATE_PATTERN.containsMatchIn(lines[i])) {
                if (i + 1 < lines.size) {
                    return lines[i + 1]
                }
            }
        }
        return null
    }

    /**
     * 형식 3 (중간형) 파싱
     *
     * 금액+일시불이 한 줄, 날짜+시간이 별도 줄에 있는 형태.
     * 사용처는 날짜/시간 줄 다음에 위치하고, 누적금액이 마지막 줄.
     */
    private fun parseFormat3(message: String): ParseResult {
        val amountMatch = FORMAT3_AMOUNT_PATTERN.find(message)
            ?: return ParseResult.failure("금액을 추출할 수 없습니다")

        val amount = parseDecimal(amountMatch.groupValues[1])
            ?: return ParseResult.failure("금액을 추출할 수 없습니다")

        val dateTimeMatch = FORMAT3_DATETIME_PATTERN.find(message)
            ?: return ParseResult.failure("거래 일시를 추출할 수 없습니다")

        val transactionDate = buildDateTime(
            month = dateTimeMatch.groupValues[1].toInt(),
            day = dateTimeMatch.groupValues[2].toInt(),
            hour = dateTimeMatch.groupValues[3].toInt(),
            minute = dateTimeMatch.groupValues[4].toInt()
        )

        val merchantName = extractFormat3Merchant(message)
            ?: return ParseResult.failure("사용처를 추출할 수 없습니다")

        val accumulatedAmount = FORMAT3_ACCUMULATED_PATTERN.find(message)?.let {
            parseDecimal(it.groupValues[1])
        }

        val cardLastFourDigits = CARD_LAST_FOUR_PATTERN.find(message)?.groupValues?.get(1)

        return ParseResult.success(
            amount = amount,
            transactionDate = transactionDate,
            merchantName = merchantName,
            accumulatedAmount = accumulatedAmount,
            cardLastFourDigits = cardLastFourDigits
        )
    }

    /**
     * 형식 3에서 사용처 추출
     *
     * 날짜+시간 줄 다음 줄이 사용처이다.
     * 단, "누적"으로 시작하는 줄은 제외한다.
     */
    private fun extractFormat3Merchant(message: String): String? {
        val lines = message.split("\n", "\r\n").map { it.trim() }.filter { it.isNotEmpty() }
        for (i in lines.indices) {
            if (FORMAT3_DATETIME_PATTERN.containsMatchIn(lines[i])) {
                if (i + 1 < lines.size) {
                    val next = lines[i + 1]
                    if (!next.startsWith("누적")) {
                        return next
                    }
                }
            }
        }
        return null
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
     * 현재 연도를 사용하며, 6개월 이상 미래인 경우에만 작년으로 처리한다.
     * (며칠~수주 차이의 SMS 지연은 올해로 유지)
     */
    private fun buildDateTime(month: Int, day: Int, hour: Int, minute: Int): LocalDateTime {
        val today = java.time.LocalDate.now()
        var year = today.year
        val date = java.time.LocalDate.of(year, month, day)
        // 6개월 이상 미래인 경우에만 작년으로 처리
        if (date.isAfter(today.plusMonths(6))) {
            year -= 1
        }
        return LocalDateTime.of(year, month, day, hour, minute)
    }
}
