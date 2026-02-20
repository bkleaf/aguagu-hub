package com.woo.server.domain.card.entity

import com.woo.server.common.enums.CardCompany
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 신용카드 엔티티
 *
 * 등록된 신용카드 정보를 관리합니다.
 * PK는 "{카드사}_{끝4자리}" 형식입니다. (예: SAMSUNG_4300)
 */
@Entity
@Table(
    name = "credit_cards",
    schema = "aguagu",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_credit_card_company_digits",
            columnNames = ["cardCompany", "lastFourDigits"]
        )
    ]
)
class CreditCard(
    /** 고유 식별자 (예: SAMSUNG_4300) */
    @Id
    @Column(length = 30)
    val id: String,

    /** 카드사 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val cardCompany: CardCompany,

    /** 카드 끝 4자리 */
    @Column(nullable = false, length = 4)
    val lastFourDigits: String,

    /** 정산 시작일 */
    @Column(nullable = false)
    var billingStartDay: Int = 1,

    /** 정산 종료일 */
    @Column(nullable = false)
    var billingEndDay: Int = 31,

    /** 카드 결제일 (매월 N일) */
    @Column
    var paymentDay: Int? = null,

    /** 사용 여부 */
    @Column(nullable = false)
    var active: Boolean = true,

    /** 생성 일시 */
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /** 수정 일시 */
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        /** ID를 카드사와 끝4자리로 생성합니다. */
        fun generateId(cardCompany: CardCompany, lastFourDigits: String): String =
            "${cardCompany.name}_$lastFourDigits"
    }
}
