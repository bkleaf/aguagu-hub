package com.woo.server.domain.card.entity

import com.woo.server.common.enums.CardCompany
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 카드 문자 파싱 실패 내역 엔티티
 *
 * 파싱에 실패한 카드 결제 문자를 저장합니다.
 */
@Entity
@Table(
    name = "card_parse_failures",
    schema = "aguagu",
    indexes = [
        Index(name = "idx_parse_failure_created_at", columnList = "createdAt"),
        Index(name = "idx_parse_failure_card_company", columnList = "cardCompany")
    ]
)
class CardParseFailure(
    /** 고유 식별자 (자동 생성) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /** 발신자 전화번호 */
    @Column(length = 20)
    val phoneNumber: String? = null,

    /** 감지된 카드사 (감지 실패 시 UNKNOWN) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val cardCompany: CardCompany,

    /** 원본 문자 메시지 */
    @Column(nullable = false, columnDefinition = "TEXT")
    val rawMessage: String,

    /** 파싱 실패 사유 */
    @Column(nullable = false, length = 500)
    val failReason: String,

    /** 생성 일시 */
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
