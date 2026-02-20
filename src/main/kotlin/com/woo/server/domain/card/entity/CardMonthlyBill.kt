package com.woo.server.domain.card.entity

import com.woo.server.common.enums.CardCompany
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 월별 청구서 엔티티
 *
 * 카드사에서 수신한 월별 청구 문자를 저장하는 엔티티입니다.
 * 결제일, 청구금액, 기준일 정보를 포함합니다.
 */
@Entity
@Table(
    name = "card_monthly_bills",
    schema = "aguagu",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_monthly_bill",
            columnNames = ["cardCompany", "billingDate", "referenceDate"]
        )
    ]
)
class CardMonthlyBill(
    /** 고유 식별자 (자동 생성) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /** 발신자 전화번호 */
    @Column(length = 20)
    val phoneNumber: String? = null,

    /** 카드사 종류 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val cardCompany: CardCompany,

    /** 결제일 */
    @Column(nullable = false)
    val billingDate: LocalDate,

    /** 청구 금액 */
    @Column(nullable = false, precision = 15, scale = 2)
    val billingAmount: BigDecimal,

    /** 기준일 */
    @Column(nullable = false)
    val referenceDate: LocalDate,

    /** 원본 문자 메시지 */
    @Column(nullable = false, columnDefinition = "TEXT")
    val rawMessage: String,

    /** 생성 일시 */
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /** 수정 일시 */
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
