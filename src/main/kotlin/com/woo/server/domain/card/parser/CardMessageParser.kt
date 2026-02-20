package com.woo.server.domain.card.parser

import com.woo.server.common.enums.CardCompany
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 카드 문자 메시지 파싱 결과를 담는 데이터 클래스
 *
 * 파싱에 성공한 경우 거래 정보를, 실패한 경우 실패 사유를 포함합니다.
 */
data class ParseResult(
    /** 파싱 성공 여부 */
    val success: Boolean,

    /** 결제 금액 */
    val amount: BigDecimal? = null,

    /** 거래 일시 */
    val transactionDate: LocalDateTime? = null,

    /** 사용처 (가맹점명) */
    val merchantName: String? = null,

    /** 누적 사용 금액 */
    val accumulatedAmount: BigDecimal? = null,

    /** 카드 끝 4자리 */
    val cardLastFourDigits: String? = null,

    /** 파싱 실패 사유 */
    val failReason: String? = null
) {
    companion object {
        /**
         * 파싱 성공 결과 생성
         */
        fun success(
            amount: BigDecimal,
            transactionDate: LocalDateTime,
            merchantName: String,
            accumulatedAmount: BigDecimal? = null,
            cardLastFourDigits: String? = null
        ): ParseResult {
            return ParseResult(
                success = true,
                amount = amount,
                transactionDate = transactionDate,
                merchantName = merchantName,
                accumulatedAmount = accumulatedAmount,
                cardLastFourDigits = cardLastFourDigits
            )
        }

        /**
         * 파싱 실패 결과 생성
         */
        fun failure(reason: String): ParseResult {
            return ParseResult(
                success = false,
                failReason = reason
            )
        }
    }
}

/**
 * 카드 문자 메시지 파서 인터페이스
 *
 * 각 카드사별 문자 메시지 파싱을 위한 공통 인터페이스입니다.
 * 새로운 카드사 파서를 추가할 때 이 인터페이스를 구현합니다.
 */
interface CardMessageParser {

    /**
     * 이 파서가 지원하는 카드사를 반환합니다.
     *
     * @return 지원하는 카드사
     */
    fun getSupportedCardCompany(): CardCompany

    /**
     * 주어진 메시지가 이 파서로 파싱 가능한지 확인합니다.
     *
     * @param message 카드 결제 문자 메시지
     * @return 파싱 가능 여부
     */
    fun canParse(message: String): Boolean

    /**
     * 문자 메시지를 파싱하여 거래 정보를 추출합니다.
     *
     * @param message 카드 결제 문자 메시지
     * @return 파싱 결과 (성공 시 거래 정보, 실패 시 실패 사유 포함)
     */
    fun parse(message: String): ParseResult
}
