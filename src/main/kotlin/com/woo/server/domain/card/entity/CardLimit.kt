package com.woo.server.domain.card.entity

import com.woo.server.common.enums.CardCompany
import com.woo.server.common.enums.LimitType
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "card_limits",
    schema = "aguagu",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_card_limit",
            columnNames = ["cardCompany", "cardLastFourDigits"]
        )
    ]
)
class CardLimit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val cardCompany: CardCompany,

    @Column(nullable = false, length = 4)
    val cardLastFourDigits: String,

    /** 한도 유형 (MONTHLY, YEARLY, CUSTOM) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var limitType: LimitType = LimitType.MONTHLY,

    /** 한도 금액 (기존 monthlyLimit에서 변경) */
    @Column(nullable = false, precision = 15, scale = 2)
    var limitAmount: BigDecimal,

    /** 커스텀 기간 시작일 (limitType이 CUSTOM일 때 사용) */
    @Column
    var customStartDate: LocalDate? = null,

    /** 커스텀 기간 종료일 (limitType이 CUSTOM일 때 사용) */
    @Column
    var customEndDate: LocalDate? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
