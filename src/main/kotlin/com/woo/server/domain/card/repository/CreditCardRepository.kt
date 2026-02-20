package com.woo.server.domain.card.repository

import com.woo.server.common.enums.CardCompany
import com.woo.server.domain.card.entity.CreditCard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * 신용카드 리포지토리
 */
@Repository
interface CreditCardRepository : JpaRepository<CreditCard, String> {

    /** 카드사와 끝4자리로 신용카드를 조회합니다. */
    fun findByCardCompanyAndLastFourDigits(
        cardCompany: CardCompany,
        lastFourDigits: String
    ): CreditCard?

    /** 카드사와 끝4자리 변형 목록으로 신용카드를 조회합니다. (마스킹 매칭용) */
    fun findByCardCompanyAndLastFourDigitsIn(
        cardCompany: CardCompany,
        lastFourDigits: List<String>
    ): List<CreditCard>

    /** 활성 상태인 신용카드 목록을 조회합니다. */
    fun findByActiveTrue(): List<CreditCard>

    /** 카드사별 신용카드 목록을 조회합니다. */
    fun findByCardCompany(cardCompany: CardCompany): List<CreditCard>

    /** 카드사와 끝4자리 LIKE 패턴으로 신용카드를 조회합니다. (마스킹 역매칭용) */
    @Query("SELECT c FROM CreditCard c WHERE c.cardCompany = :cardCompany AND c.lastFourDigits LIKE :pattern")
    fun findByCardCompanyAndLastFourDigitsLike(
        @Param("cardCompany") cardCompany: CardCompany,
        @Param("pattern") pattern: String
    ): List<CreditCard>
}
