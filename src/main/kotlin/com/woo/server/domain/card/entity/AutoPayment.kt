package com.woo.server.domain.card.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 자동결제 엔티티
 *
 * 카드별로 매월 반복되는 정기 결제(넷플릭스, 보험료, 통신비 등)를 관리합니다.
 * 등록된 CreditCard에 연결하여 한도 사용액 계산 시 합산합니다.
 */
@Entity
@Table(name = "auto_payments", schema = "aguagu")
class AutoPayment(
    /** 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /** 연결된 신용카드 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_card_id", nullable = false)
    val creditCard: CreditCard,

    /** 자동결제 설명 (예: 넷플릭스, SKT) */
    @Column(nullable = false, length = 100)
    var description: String,

    /** 결제 금액 */
    @Column(nullable = false, precision = 15, scale = 2)
    var amount: BigDecimal,

    /** 매월 결제일 (1~31) */
    @Column(nullable = false)
    var paymentDay: Int,

    /** 활성화 여부 */
    @Column(nullable = false)
    var active: Boolean = true,

    /** 생성 일시 */
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /** 수정 일시 */
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
