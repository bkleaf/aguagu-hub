package com.woo.server.domain.card.parser

import com.woo.server.common.enums.CardCompany
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.Year

/**
 * 삼성카드 문자 메시지 파서
 *
 * 삼성카드의 결제 승인 문자를 파싱하여 거래 정보를 추출합니다.
 * 개인카드, 법인카드, 후불교통, 자동결제 네 가지 형식을 지원합니다.
 *
 * 형식 1 (법인카드):
 * ```
 * 삼성법인4300
 * 766,120원 일시불
 * 01/25 19:44 서울시네이버페이
 * 잔여한도2,663,880원
 * ```
 *
 * 형식 2 (개인카드):
 * ```
 * 삼성6518승인 전*우
 * 4,000원 일시불
 * 01/30 17:16 더열린이비인후과
 * 누적3,600,217원
 * ```
 *
 * 형식 3 (후불교통):
 * ```
 * [삼성카드]6518
 * 02월접수 후불교통
 * (버스+지하철+통행료)
 * 합계 74,320원
 * ```
 *
 * 형식 4 (자동결제):
 * ```
 * [삼성카드]6518
 * 자동결제 02/24접수
 * 아파트관리비
 * 159,710원
 * ```
 */
@Component
class SamsungCardParser : CardMessageParser {

    companion object {
        /** 삼성카드 문자 식별 키워드 */
        private val SAMSUNG_KEYWORDS = listOf("삼성", "SAMSUNG")

        /** 금액 추출 정규식 (예: "4,500원", "766,120원") */
        private val AMOUNT_PATTERN = Regex("""([0-9,]+)원\s*(일시불|할부)?""")

        /** 날짜/시간 추출 정규식 (예: "01/30 17:16") */
        private val DATE_TIME_PATTERN = Regex("""(\d{1,2})/(\d{1,2})\s+(\d{1,2}):(\d{2})""")

        /** 날짜/시간 이후 같은 줄의 사용처 추출 (예: "01/25 19:44 서울시네이버페이") */
        private val DATETIME_MERCHANT_PATTERN = Regex("""\d{1,2}/\d{1,2}\s+\d{1,2}:\d{2}\s+(.+)""")

        /** 누적 금액 추출 정규식 (예: "누적773,774" 또는 "누적3,600,217원") */
        private val ACCUMULATED_PATTERN = Regex("""누적\s*([0-9,]+)""")

        /** 잔여한도 추출 정규식 (예: "잔여한도2,663,880원") */
        private val REMAINING_LIMIT_PATTERN = Regex("""잔여한도\s*([0-9,]+)""")

        /** 카드 끝4자리 추출 정규식 (예: "삼성9684", "삼성법인4300") */
        private val CARD_LAST_FOUR_PATTERN = Regex("""삼성(?:법인)?(\d{4})""")

        // === 후불교통 형식 정규식 ===

        /** 후불교통 카드번호 추출 (예: "[삼성카드]6518") */
        private val POSTPAID_CARD_PATTERN = Regex("""\[삼성카드\](\d{4})""")

        /** 후불교통 금액 추출 (예: "합계 74,320원") */
        private val POSTPAID_AMOUNT_PATTERN = Regex("""합계\s*([0-9,]+)원""")

        /** 후불교통 사용처 추출 (예: "02월접수 후불교통") */
        private val POSTPAID_MERCHANT_PATTERN = Regex("""(\d{2}월접수\s*후불교통)""")

        // === 자동결제 형식 정규식 ===

        /** 자동결제 날짜 추출 (예: "자동결제 02/24접수") */
        private val AUTO_PAYMENT_DATE_PATTERN = Regex("""자동결제\s+(\d{1,2})/(\d{1,2})접수""")

        // === 취소 형식 정규식 ===

        /** 취소 카드번호 추출 (예: "[삼성카드]6518취소") */
        private val CANCEL_CARD_PATTERN = Regex("""\[삼성카드\](\d{4})취소""")

        /** 취소 날짜 + 사용처 추출 (예: "02/18 네이버페이") */
        private val CANCEL_DATE_MERCHANT_PATTERN = Regex("""(\d{1,2})/(\d{1,2})\s+(.+)""")

        /** 취소 금액 추출 (예: "-51,840원") */
        private val CANCEL_AMOUNT_PATTERN = Regex("""-([0-9,]+)원""")
    }

    /**
     * 삼성카드를 지원 카드사로 반환
     */
    override fun getSupportedCardCompany(): CardCompany = CardCompany.SAMSUNG

    /**
     * 메시지가 삼성카드 문자인지 확인
     *
     * "승인", "법인", "후불교통", "자동결제", "취소" 키워드가 포함된 삼성카드 문자를 식별한다.
     *
     * @param message 문자 메시지
     * @return 삼성카드 문자 여부
     */
    override fun canParse(message: String): Boolean {
        val hasSamsungKeyword = SAMSUNG_KEYWORDS.any { keyword ->
            message.contains(keyword, ignoreCase = true)
        }
        val isApproval = message.contains("승인") || message.contains("법인")
        val isPostpaidTransport = message.contains("후불교통")
        val isAutoPayment = message.contains("자동결제")
        val isCancel = CANCEL_CARD_PATTERN.containsMatchIn(message)
        return hasSamsungKeyword && (isApproval || isPostpaidTransport || isAutoPayment || isCancel)
    }

    /**
     * 삼성카드 문자 메시지를 파싱하여 거래 정보 추출
     *
     * @param message 삼성카드 결제 승인 문자
     * @return 파싱 결과 (금액, 일시, 사용처, 누적금액)
     */
    override fun parse(message: String): ParseResult {
        return try {
            if (CANCEL_CARD_PATTERN.containsMatchIn(message)) {
                parseCancel(message)
            } else if (message.contains("후불교통")) {
                parsePostpaidTransport(message)
            } else if (message.contains("자동결제")) {
                parseAutoPayment(message)
            } else {
                parseApproval(message)
            }
        } catch (e: Exception) {
            ParseResult.failure("파싱 중 오류 발생: ${e.message}")
        }
    }

    /**
     * 취소 문자 파싱
     *
     * 형식:
     * ```
     * [Web발신]
     * [삼성카드]6518취소
     * 02/18 네이버페이
     * -51,840원
     * q.scqr.kr/4BIelK6
     * ```
     */
    private fun parseCancel(message: String): ParseResult {
        // 카드 끝4자리 추출 ([삼성카드]XXXX취소)
        val cardLastFourDigits = CANCEL_CARD_PATTERN.find(message)?.groupValues?.get(1)
            ?: return ParseResult.failure("취소 카드번호를 추출할 수 없습니다")

        // 금액 추출 (-XX,XXX원)
        val amount = CANCEL_AMOUNT_PATTERN.find(message)?.let {
            parseDecimal(it.groupValues[1])
        } ?: return ParseResult.failure("취소 금액을 추출할 수 없습니다")

        // 날짜 + 사용처 추출 (MM/DD 사용처)
        val lines = message.split("\n", "\r\n").map { it.trim() }.filter { it.isNotEmpty() }
        var transactionDate: LocalDateTime? = null
        var merchantName: String? = null

        for (line in lines) {
            val match = CANCEL_DATE_MERCHANT_PATTERN.find(line)
            if (match != null && !line.contains("취소") && !line.contains("삼성카드")) {
                val month = match.groupValues[1].toInt()
                val day = match.groupValues[2].toInt()
                merchantName = match.groupValues[3].trim()

                val today = java.time.LocalDate.now()
                var year = today.year
                val date = java.time.LocalDate.of(year, month, day)
                if (date.isAfter(today.plusMonths(6))) {
                    year -= 1
                }
                transactionDate = LocalDateTime.of(year, month, day, 0, 0)
                break
            }
        }

        if (transactionDate == null) return ParseResult.failure("취소 거래 일시를 추출할 수 없습니다")
        if (merchantName == null) return ParseResult.failure("취소 사용처를 추출할 수 없습니다")

        return ParseResult.success(
            amount = amount,
            transactionDate = transactionDate,
            merchantName = merchantName,
            accumulatedAmount = null,
            cardLastFourDigits = cardLastFourDigits,
            cancelled = true
        )
    }

    /**
     * 일반 승인 문자 파싱 (개인카드, 법인카드)
     */
    private fun parseApproval(message: String): ParseResult {
        // 금액 추출
        val amount = extractAmount(message)
            ?: return ParseResult.failure("금액을 추출할 수 없습니다")

        // 거래 일시 추출
        val transactionDate = extractDateTime(message)
            ?: return ParseResult.failure("거래 일시를 추출할 수 없습니다")

        // 사용처 추출
        val merchantName = extractMerchantName(message)
            ?: return ParseResult.failure("사용처를 추출할 수 없습니다")

        // 누적 금액 또는 잔여한도 추출 (선택적)
        val accumulatedAmount = extractAccumulatedAmount(message)

        // 카드 끝4자리 추출 (선택적)
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
     * 후불교통 문자 파싱
     *
     * 형식:
     * ```
     * [삼성카드]6518
     * 02월접수 후불교통
     * (버스+지하철+통행료)
     * 합계 74,320원
     * ```
     */
    private fun parsePostpaidTransport(message: String): ParseResult {
        // 금액 추출 (합계 XX,XXX원)
        val amount = POSTPAID_AMOUNT_PATTERN.find(message)?.let {
            parseDecimal(it.groupValues[1])
        } ?: return ParseResult.failure("금액을 추출할 수 없습니다")

        // 카드 끝4자리 추출 ([삼성카드]XXXX)
        val cardLastFourDigits = POSTPAID_CARD_PATTERN.find(message)?.groupValues?.get(1)

        // 사용처 추출 (XX월접수 후불교통 + 상세내역)
        val merchantName = extractPostpaidMerchantName(message)
            ?: return ParseResult.failure("사용처를 추출할 수 없습니다")

        // 거래 일시 (후불교통은 날짜 정보가 없으므로 현재 시간 사용)
        val transactionDate = LocalDateTime.now()

        return ParseResult.success(
            amount = amount,
            transactionDate = transactionDate,
            merchantName = merchantName,
            accumulatedAmount = null,
            cardLastFourDigits = cardLastFourDigits
        )
    }

    /**
     * 후불교통 사용처 추출
     *
     * "02월접수 후불교통 (버스+지하철+통행료)" 형태로 반환
     */
    private fun extractPostpaidMerchantName(message: String): String? {
        val lines = message.split("\n", "\r\n").map { it.trim() }.filter { it.isNotEmpty() }

        // "XX월접수 후불교통" 라인 찾기
        val merchantLineIndex = lines.indexOfFirst { it.contains("후불교통") }
        if (merchantLineIndex == -1) return null

        val merchantLine = lines[merchantLineIndex]

        // 다음 줄에 상세 내역이 있으면 합침 (예: "(버스+지하철+통행료)")
        val detailLine = if (merchantLineIndex + 1 < lines.size) {
            val nextLine = lines[merchantLineIndex + 1]
            if (nextLine.startsWith("(") && !nextLine.contains("합계")) nextLine else null
        } else null

        return if (detailLine != null) {
            "$merchantLine $detailLine"
        } else {
            merchantLine
        }
    }

    /**
     * 자동결제 문자 파싱
     *
     * 형식:
     * ```
     * [삼성카드]6518
     * 자동결제 02/24접수
     * 아파트관리비
     * 159,710원
     * ```
     */
    private fun parseAutoPayment(message: String): ParseResult {
        // 금액 추출
        val amount = extractAmount(message)
            ?: return ParseResult.failure("금액을 추출할 수 없습니다")

        // 카드 끝4자리 추출 ([삼성카드]XXXX 또는 삼성XXXX)
        val cardLastFourDigits = POSTPAID_CARD_PATTERN.find(message)?.groupValues?.get(1)
            ?: CARD_LAST_FOUR_PATTERN.find(message)?.groupValues?.get(1)

        // 거래 일시 추출 (자동결제 MM/DD접수 → 시간 없이 00:00으로 설정)
        val transactionDate = extractAutoPaymentDate(message)
            ?: return ParseResult.failure("거래 일시를 추출할 수 없습니다")

        // 사용처 추출 (자동결제 라인 다음, 금액 라인 이전)
        val merchantName = extractAutoPaymentMerchantName(message)
            ?: return ParseResult.failure("사용처를 추출할 수 없습니다")

        return ParseResult.success(
            amount = amount,
            transactionDate = transactionDate,
            merchantName = merchantName,
            accumulatedAmount = null,  // 자동결제는 누적 사용액 없음
            cardLastFourDigits = cardLastFourDigits
        )
    }

    /**
     * 자동결제 문자에서 거래 일시를 추출
     *
     * "자동결제 02/24접수" 형식에서 월/일을 추출하고,
     * 시간 정보가 없으므로 00:00으로 설정한다.
     * 연도는 현재 연도 기준, 6개월 이상 미래이면 작년으로 처리.
     *
     * @param message 문자 메시지
     * @return 거래 일시 (LocalDateTime), 추출 실패 시 null
     */
    private fun extractAutoPaymentDate(message: String): LocalDateTime? {
        val match = AUTO_PAYMENT_DATE_PATTERN.find(message) ?: return null

        return try {
            val month = match.groupValues[1].toInt()
            val day = match.groupValues[2].toInt()

            val today = java.time.LocalDate.now()
            var year = today.year
            val date = java.time.LocalDate.of(year, month, day)

            // 6개월 이상 미래인 경우에만 작년으로 처리
            // (며칠~수주 차이의 SMS 지연은 올해로 유지)
            if (date.isAfter(today.plusMonths(6))) {
                year -= 1
            }

            LocalDateTime.of(year, month, day, 0, 0)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 자동결제 문자에서 사용처(가맹점명)를 추출
     *
     * "자동결제" 키워드가 포함된 라인 다음, 금액 라인 이전의 텍스트를 사용처로 추출한다.
     *
     * @param message 문자 메시지
     * @return 사용처명, 추출 실패 시 null
     */
    private fun extractAutoPaymentMerchantName(message: String): String? {
        val lines = message.split("\n", "\r\n").map { it.trim() }.filter { it.isNotEmpty() }

        // "자동결제" 라인 찾기
        val autoPaymentLineIndex = lines.indexOfFirst { it.contains("자동결제") }
        if (autoPaymentLineIndex == -1) return null

        // 자동결제 라인 다음 줄이 사용처 (금액 라인이 아닌 경우)
        val nextIndex = autoPaymentLineIndex + 1
        if (nextIndex < lines.size && !AMOUNT_PATTERN.containsMatchIn(lines[nextIndex])) {
            return lines[nextIndex]
        }

        return null
    }

    /**
     * 문자 메시지에서 결제 금액을 추출
     *
     * "잔여한도", "누적" 뒤의 금액은 제외하고 실제 결제 금액만 추출한다.
     *
     * @param message 문자 메시지
     * @return 결제 금액 (BigDecimal), 추출 실패 시 null
     */
    private fun extractAmount(message: String): BigDecimal? {
        // 각 줄에서 금액을 찾되, "잔여한도"나 "누적"이 포함된 줄은 제외
        val lines = message.split("\n", "\r\n").map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines) {
            if (line.startsWith("잔여한도") || line.startsWith("누적")) continue
            val match = AMOUNT_PATTERN.find(line) ?: continue
            val amountStr = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amountStr)
            } catch (e: NumberFormatException) {
                null
            }
        }
        return null
    }

    /**
     * 문자 메시지에서 거래 일시를 추출
     *
     * 문자에는 연도가 포함되지 않으므로 현재 연도를 사용합니다.
     * 단, 6개월 이상 미래인 경우에만 작년으로 처리합니다.
     *
     * @param message 문자 메시지
     * @return 거래 일시 (LocalDateTime), 추출 실패 시 null
     */
    private fun extractDateTime(message: String): LocalDateTime? {
        val match = DATE_TIME_PATTERN.find(message) ?: return null

        return try {
            val month = match.groupValues[1].toInt()
            val day = match.groupValues[2].toInt()
            val hour = match.groupValues[3].toInt()
            val minute = match.groupValues[4].toInt()

            val today = java.time.LocalDate.now()
            var year = today.year
            val date = java.time.LocalDate.of(year, month, day)

            // 6개월 이상 미래인 경우에만 작년으로 처리
            // (며칠~수주 차이의 SMS 지연은 올해로 유지)
            if (date.isAfter(today.plusMonths(6))) {
                year -= 1
            }

            LocalDateTime.of(year, month, day, hour, minute)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 문자 메시지에서 사용처(가맹점명)를 추출
     *
     * 두 가지 경우를 처리한다:
     * 1. 날짜/시간과 같은 줄에 사용처가 있는 경우 (예: "01/25 19:44 서울시네이버페이")
     * 2. 날짜/시간 다음 줄에 사용처가 있는 경우 (예: 기존 형식)
     *
     * @param message 문자 메시지
     * @return 사용처명, 추출 실패 시 null
     */
    private fun extractMerchantName(message: String): String? {
        val lines = message.split("\n", "\r\n").map { it.trim() }.filter { it.isNotEmpty() }

        for (i in lines.indices) {
            if (DATE_TIME_PATTERN.containsMatchIn(lines[i])) {
                // 1. 같은 줄에 날짜/시간 뒤에 사용처가 있는 경우
                val sameLine = DATETIME_MERCHANT_PATTERN.find(lines[i])
                if (sameLine != null) {
                    return sameLine.groupValues[1].trim()
                }

                // 2. 다음 줄이 사용처인 경우 (누적/잔여한도가 아닌 줄)
                if (i + 1 < lines.size
                    && !lines[i + 1].startsWith("누적")
                    && !lines[i + 1].startsWith("잔여한도")
                ) {
                    return lines[i + 1]
                }
            }
        }

        return null
    }

    /**
     * 문자 메시지에서 누적 사용 금액 또는 잔여한도를 추출
     *
     * "누적" 또는 "잔여한도" 패턴을 모두 지원한다.
     *
     * @param message 문자 메시지
     * @return 누적 금액 (BigDecimal), 추출 실패 시 null
     */
    private fun extractAccumulatedAmount(message: String): BigDecimal? {
        // 누적 금액 먼저 확인
        val accMatch = ACCUMULATED_PATTERN.find(message)
        if (accMatch != null) {
            return parseDecimal(accMatch.groupValues[1])
        }

        // 잔여한도 확인
        val remMatch = REMAINING_LIMIT_PATTERN.find(message)
        if (remMatch != null) {
            return parseDecimal(remMatch.groupValues[1])
        }

        return null
    }

    /**
     * 카드 끝4자리 추출 (삼성XXXX 또는 삼성법인XXXX 형식)
     */
    private fun extractCardLastFourDigits(message: String): String? {
        return CARD_LAST_FOUR_PATTERN.find(message)?.groupValues?.get(1)
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
}
