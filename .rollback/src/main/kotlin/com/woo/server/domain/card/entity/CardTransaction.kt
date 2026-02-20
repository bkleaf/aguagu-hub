package com.woo.server.domain.card.entity

import com.woo.server.common.enums.CardCompany
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 카드 거래 내역 엔티티
 *
 * 신용카드 결제 문자를 파싱하여 저장하는 엔티티입니다.
 * 각 거래의 금액, 일시, 사용처 등의 정보를 저장합니다.
 */
@Entity
@Table(
    name = "card_transactions",
    schema = "aguagu",
    indexes = [
        Index(name = "idx_card_transaction_date", columnList = "transactionDate"),
        Index(name = "idx_card_company", columnList = "cardCompany"),
        Index(name = "idx_parse_success", columnList = "parseSuccess")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_card_transaction_unique",
            columnNames = ["cardCompany", "amount", "transactionDate", "merchantName"]
        )
    ]
)
class CardTransaction(
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

    /** 카드 끝 4자리 */
    @Column(length = 4)
    val cardLastFourDigits: String? = null,

    /** 결제 금액 */
    @Column(nullable = false, precision = 15, scale = 2)
    val amount: BigDecimal,

    /** 거래 일시 */
    @Column(nullable = false)
    val transactionDate: LocalDateTime,

    /** 사용처 (가맹점명) */
    @Column(nullable = false, length = 100)
    val merchantName: String,

    /** 누적 사용 금액 (nullable - 일부 카드사는 제공하지 않음) */
    @Column(precision = 15, scale = 2)
    val accumulatedAmount: BigDecimal? = null,

    /** 원본 문자 메시지 */
    @Column(nullable = false, columnDefinition = "TEXT")
    val rawMessage: String,

    /** 파싱 성공 여부 */
    @Column(nullable = false)
    val parseSuccess: Boolean = true,

    /** 파싱 실패 사유 (파싱 실패 시에만 저장) */
    @Column(length = 500)
    val parseFailReason: String? = null,

    /** 생성 일시 */
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * 파싱 실패 거래 생성을 위한 팩토리 메서드
     *
     * @param rawMessage 원본 문자 메시지
     * @param cardCompany 감지된 카드사 (감지 실패 시 UNKNOWN)
     * @param failReason 파싱 실패 사유
     * @return 파싱 실패 상태의 CardTransaction 엔티티
     */
    companion object {
        fun createFailedTransaction(
            phoneNumber: String,
            rawMessage: String,
            cardCompany: CardCompany,
            failReason: String
        ): CardTransaction {
            return CardTransaction(
                phoneNumber = phoneNumber,
                cardCompany = cardCompany,
                amount = BigDecimal.ZERO,
                transactionDate = LocalDateTime.now(),
                merchantName = "파싱 실패",
                rawMessage = rawMessage,
                parseSuccess = false,
                parseFailReason = failReason
            )
        }
    }
}
