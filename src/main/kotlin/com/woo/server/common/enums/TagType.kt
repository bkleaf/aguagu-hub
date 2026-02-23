package com.woo.server.common.enums

/**
 * 태그 유형 열거형
 *
 * 거래에 할당된 태그의 유형을 구분합니다.
 * - MAIN: 주요 태그 (거래당 최대 1개)
 * - DETAIL: 세부 태그 (거래당 0개 이상)
 */
enum class TagType(
    /** 한글 표시명 */
    val displayName: String
) {
    /** 주요 태그 (거래당 최대 1개) */
    MAIN("주요"),
    /** 세부 태그 (거래당 0개 이상) */
    DETAIL("세부")
}
