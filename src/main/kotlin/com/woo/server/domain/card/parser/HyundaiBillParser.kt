package com.woo.server.domain.card.parser

import com.woo.server.common.enums.CardCompany
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 현대카드 월별 청구서 문자 파서
 *
 * 현대카드의 월별 결제 청구 문자를 파싱합니다.
 *
 * 형식 예시:
 * ```
 * [현대카드] 전*우님 03/12 결제금액 9,134원(03/02기준) 신한은행 h7.is/앱명세서
 * ```
 */
@Component
class HyundaiBillParser : BillMessageParser {

    companion object {
        /**
         * 현대카드 청구서 정규식
         * 그룹: (결제월)/(결제일) 결제금액 (금액)원((기준월)/(기준일)기준)
         */
        private val BILL_PATTERN = Regex(
            """(\d{1,2})/(\d{1,2})\s*결제금액\s*([0-9,]+)원\s*\((\d{1,2})/(\d{1,2})기준\)"""
        )
    }

    /** 현대카드를 지원 카드사로 반환 */
    override fun getSupportedCardCompany(): CardCompany = CardCompany.HYUNDAI

    /**
     * 메시지가 현대카드 청구서 문자인지 확인
     *
     * @param message 문자 메시지
     * @return 현대카드 청구서 여부
     */
    override fun canParse(message: String): Boolean {
        return BILL_PATTERN.containsMatchIn(message)
    }

    /**
     * 현대카드 청구서 문자 파싱
     *
     * @param message 현대카드 청구서 문자
     * @return 파싱 결과 (결제일, 청구금액, 기준일)
     */
    override fun parse(message: String): BillParseResult {
        return try {
            val match = BILL_PATTERN.find(message)
                ?: return BillParseResult.failure("현대카드 청구서 형식을 인식할 수 없습니다")

            val billingMonth = match.groupValues[1].toInt()
            val billingDay = match.groupValues[2].toInt()
            val amountStr = match.groupValues[3].replace(",", "")
            val refMonth = match.groupValues[4].toInt()
            val refDay = match.groupValues[5].toInt()

            val year = resolveYear(billingMonth)
            val billingDate = LocalDate.of(year, billingMonth, billingDay)
            val billingAmount = BigDecimal(amountStr)
            val referenceDate = resolveReferenceDate(refMonth, refDay, billingDate)

            BillParseResult.success(billingDate, billingAmount, referenceDate)
        } catch (e: Exception) {
            BillParseResult.failure("현대카드 청구서 파싱 중 오류: ${e.message}")
        }
    }

    /**
     * 결제 월로부터 연도를 추정합니다.
     */
    private fun resolveYear(month: Int): Int {
        val today = LocalDate.now()
        return if (month > today.monthValue + 1) today.year - 1 else today.year
    }

    /**
     * 기준일의 연도를 결제일 기준으로 추정합니다.
     */
    private fun resolveReferenceDate(refMonth: Int, refDay: Int, billingDate: LocalDate): LocalDate {
        val year = if (refMonth > billingDate.monthValue) billingDate.year - 1 else billingDate.year
        return LocalDate.of(year, refMonth, refDay)
    }
}
