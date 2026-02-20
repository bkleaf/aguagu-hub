package com.woo.server.domain.card.parser

import com.woo.server.common.enums.CardCompany
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 월별 청구서 파싱 결과를 담는 데이터 클래스
 *
 * 청구서 문자에서 추출한 결제일, 청구금액, 기준일 정보를 포함합니다.
 */
data class BillParseResult(
    /** 파싱 성공 여부 */
    val success: Boolean,

    /** 결제일 (MM/dd → LocalDate) */
    val billingDate: LocalDate? = null,

    /** 청구 금액 */
    val billingAmount: BigDecimal? = null,

    /** 기준일 (MM/dd → LocalDate) */
    val referenceDate: LocalDate? = null,

    /** 파싱 실패 사유 */
    val failReason: String? = null
) {
    companion object {
        /**
         * 파싱 성공 결과 생성
         */
        fun success(
            billingDate: LocalDate,
            billingAmount: BigDecimal,
            referenceDate: LocalDate
        ): BillParseResult {
            return BillParseResult(
                success = true,
                billingDate = billingDate,
                billingAmount = billingAmount,
                referenceDate = referenceDate
            )
        }

        /**
         * 파싱 실패 결과 생성
         */
        fun failure(reason: String): BillParseResult {
            return BillParseResult(
                success = false,
                failReason = reason
            )
        }
    }
}

/**
 * 월별 청구서 문자 파서 인터페이스
 *
 * 각 카드사별 월별 청구서 문자 파싱을 위한 공통 인터페이스입니다.
 */
interface BillMessageParser {

    /**
     * 이 파서가 지원하는 카드사를 반환합니다.
     *
     * @return 지원하는 카드사
     */
    fun getSupportedCardCompany(): CardCompany

    /**
     * 주어진 메시지가 이 파서로 파싱 가능한지 확인합니다.
     *
     * @param message 청구서 문자 메시지
     * @return 파싱 가능 여부
     */
    fun canParse(message: String): Boolean

    /**
     * 청구서 문자 메시지를 파싱하여 청구 정보를 추출합니다.
     *
     * @param message 청구서 문자 메시지
     * @return 파싱 결과 (성공 시 청구 정보, 실패 시 실패 사유 포함)
     */
    fun parse(message: String): BillParseResult
}
