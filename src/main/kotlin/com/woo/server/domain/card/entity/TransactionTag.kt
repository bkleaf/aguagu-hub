package com.woo.server.domain.card.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 거래-태그 중간 엔티티 (N:M)
 *
 * 카드 거래와 태그의 다대다 관계를 매핑합니다.
 * 하나의 거래에 여러 태그를, 하나의 태그를 여러 거래에 부여할 수 있습니다.
 */
@Entity
@Table(
    name = "transaction_tags",
    schema = "aguagu",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_transaction_tag",
            columnNames = ["transaction_id", "tag_id"]
        )
    ]
)
class TransactionTag(
    /** 고유 식별자 (자동 생성) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /** 연결된 거래 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    val transaction: CardTransaction,

    /** 연결된 태그 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    val tag: Tag,

    /** 생성 일시 */
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
