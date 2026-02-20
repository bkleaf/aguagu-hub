package com.woo.server.domain.card.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 태그 엔티티
 *
 * 거래 내역에 부여할 수 있는 분류 태그입니다.
 * 이름과 색상(hex)을 가지며, 하나의 거래에 여러 태그를 부여할 수 있습니다 (N:M).
 */
@Entity
@Table(name = "tags", schema = "aguagu")
class Tag(
    /** 고유 식별자 (자동 생성) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /** 태그 이름 (고유) */
    @Column(nullable = false, unique = true, length = 50)
    var name: String,

    /** 태그 색상 (hex, 예: #2196F3) */
    @Column(nullable = false, length = 7)
    var color: String = "#2196F3",

    /** 생성 일시 */
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /** 수정 일시 */
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
