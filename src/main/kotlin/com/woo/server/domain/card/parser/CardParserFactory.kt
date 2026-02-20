package com.woo.server.domain.card.parser

import com.woo.server.common.enums.CardCompany
import org.springframework.stereotype.Component

/**
 * 카드 문자 파서 팩토리
 *
 * 문자 메시지에서 카드사를 자동으로 감지하고,
 * 해당 카드사에 맞는 파서를 반환하는 팩토리 클래스입니다.
 *
 * 새로운 카드사 파서를 추가하면 Spring이 자동으로 등록합니다.
 */
@Component
class CardParserFactory(
    /** 등록된 모든 카드 문자 파서 목록 (Spring이 자동 주입) */
    private val parsers: List<CardMessageParser>
) {

    /**
     * 문자 메시지에서 카드사를 감지하고 적절한 파서를 반환합니다.
     *
     * @param message 카드 결제 문자 메시지
     * @return 해당 카드사의 파서, 감지 실패 시 null
     */
    fun getParser(message: String): CardMessageParser? {
        return parsers.firstOrNull { it.canParse(message) }
    }

    /**
     * 문자 메시지에서 카드사를 감지합니다.
     *
     * @param message 카드 결제 문자 메시지
     * @return 감지된 카드사, 감지 실패 시 UNKNOWN
     */
    fun detectCardCompany(message: String): CardCompany {
        val parser = getParser(message)
        return parser?.getSupportedCardCompany() ?: CardCompany.detectFromMessage(message)
    }

    /**
     * 문자 메시지를 파싱합니다.
     *
     * 적절한 파서를 찾아 파싱을 시도하고, 파서가 없으면 실패 결과를 반환합니다.
     *
     * @param message 카드 결제 문자 메시지
     * @return 파싱 결과
     */
    fun parse(message: String): ParseResult {
        val parser = getParser(message)
            ?: return ParseResult.failure("지원하지 않는 카드사이거나 파싱할 수 없는 형식입니다")

        return parser.parse(message)
    }

    /**
     * 특정 카드사의 파서를 반환합니다.
     *
     * @param cardCompany 카드사
     * @return 해당 카드사의 파서, 없으면 null
     */
    fun getParserByCardCompany(cardCompany: CardCompany): CardMessageParser? {
        return parsers.firstOrNull { it.getSupportedCardCompany() == cardCompany }
    }

    /**
     * 현재 지원하는 카드사 목록을 반환합니다.
     *
     * @return 지원 카드사 목록
     */
    fun getSupportedCardCompanies(): List<CardCompany> {
        return parsers.map { it.getSupportedCardCompany() }
    }
}
