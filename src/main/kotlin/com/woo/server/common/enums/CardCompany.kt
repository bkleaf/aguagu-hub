package com.woo.server.common.enums

/**
 * 카드사 종류를 정의하는 열거형
 *
 * 각 카드사별 문자 메시지 패턴을 식별하기 위해 사용됩니다.
 * 새로운 카드사 추가 시 이 열거형에 추가하고, 해당 파서를 구현해야 합니다.
 */
enum class CardCompany(
    /** 카드사 한글 이름 */
    val displayName: String,
    /** 문자 메시지에서 카드사를 식별하기 위한 키워드 목록 */
    val keywords: List<String>,
    /** SMS 발신 전화번호 (거래 승인) */
    val phoneNumber: String = "",
    /** 월별 청구서 발신 전화번호 */
    val billingPhoneNumber: String = ""
) {
    /** 삼성카드 */
    SAMSUNG("삼성카드", listOf("삼성", "SAMSUNG"), "15881000", "15888700"),

    /** 현대카드 */
    HYUNDAI("현대카드", listOf("현대", "HYUNDAI"), "15772100"),

    /** KB국민카드 */
    KB("KB국민카드", listOf("KB", "국민"), "15881688", "18990800"),

    /** 신한카드 */
    SHINHAN("신한카드", listOf("신한", "SHINHAN"), "15446200"),

    /** 롯데카드 */
    LOTTE("롯데카드", listOf("롯데", "LOTTE"), "15882300"),

    /** 하나카드 */
    HANA("하나카드", listOf("하나", "HANA"), "18001111", "18001428"),

    /** 우리카드 */
    WOORI("우리카드", listOf("우리", "WOORI"), "15881600"),

    /** NH농협카드 */
    NH("NH농협카드", listOf("NH", "농협"), "15442100"),

    /** BC카드 */
    BC("BC카드", listOf("BC", "비씨"), "15880700"),

    /** 알 수 없는 카드사 */
    UNKNOWN("알 수 없음", emptyList(), "");

    companion object {
        /**
         * 문자 메시지에서 카드사를 자동으로 감지합니다.
         *
         * @param message 카드 결제 문자 메시지
         * @return 감지된 카드사, 감지 실패 시 UNKNOWN
         */
        fun detectFromMessage(message: String): CardCompany {
            return entries.firstOrNull { company ->
                company.keywords.any { keyword ->
                    message.contains(keyword, ignoreCase = true)
                }
            } ?: UNKNOWN
        }

        /**
         * 발신번호로 청구서 카드사를 감지합니다.
         *
         * @param phoneNumber SMS 발신 전화번호
         * @return 감지된 카드사, 감지 실패 시 null
         */
        fun detectBillingCompany(phoneNumber: String): CardCompany? {
            return entries.firstOrNull { company ->
                company.billingPhoneNumber.isNotEmpty() && company.billingPhoneNumber == phoneNumber
            }
        }
    }
}
