package com.woo.server.common.enums

/**
 * 카드 한도 유형 열거형
 *
 * 월간, 연간, 커스텀 기간별 한도를 구분합니다.
 */
enum class LimitType(
    /** 한도 유형 한글 이름 */
    val displayName: String
) {
    /** 월간 한도 */
    MONTHLY("월간"),

    /** 연간 한도 */
    YEARLY("연간"),

    /** 커스텀 기간 한도 */
    CUSTOM("커스텀")
}
