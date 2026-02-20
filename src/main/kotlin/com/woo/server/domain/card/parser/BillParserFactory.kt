package com.woo.server.domain.card.parser

import com.woo.server.common.enums.CardCompany
import org.springframework.stereotype.Component

/**
 * 청구서 문자 파서 팩토리
 *
 * 카드사별 청구서 문자 파서를 관리하고 적절한 파서를 반환합니다.
 * 새로운 카드사 청구서 파서를 추가하면 Spring이 자동으로 등록합니다.
 */
@Component
class BillParserFactory(
    /** 등록된 모든 청구서 파서 목록 (Spring이 자동 주입) */
    private val parsers: List<BillMessageParser>
) {

    /**
     * 카드사에 해당하는 청구서 파서를 반환합니다.
     *
     * @param cardCompany 카드사
     * @return 해당 카드사의 청구서 파서, 없으면 null
     */
    fun getParser(cardCompany: CardCompany): BillMessageParser? {
        return parsers.firstOrNull { it.getSupportedCardCompany() == cardCompany }
    }

    /**
     * 청구서 문자를 파싱합니다.
     *
     * @param cardCompany 카드사
     * @param message 청구서 문자 메시지
     * @return 파싱 결과
     */
    fun parse(cardCompany: CardCompany, message: String): BillParseResult {
        val parser = getParser(cardCompany)
            ?: return BillParseResult.failure("지원하지 않는 카드사의 청구서입니다: ${cardCompany.displayName}")

        if (!parser.canParse(message)) {
            return BillParseResult.failure("청구서 형식을 인식할 수 없습니다")
        }

        return parser.parse(message)
    }
}
