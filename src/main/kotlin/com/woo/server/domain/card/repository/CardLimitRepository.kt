package com.woo.server.domain.card.repository

import com.woo.server.common.enums.CardCompany
import com.woo.server.domain.card.entity.CardLimit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CardLimitRepository : JpaRepository<CardLimit, Long> {

    fun findByCardCompanyAndCardLastFourDigits(
        cardCompany: CardCompany,
        cardLastFourDigits: String
    ): CardLimit?

    /**
     * 카드 끝자리 여러 변형으로 한도를 조회합니다.
     *
     * 거래에 마스킹된 형태(7*2*)로 저장된 경우,
     * 한도에 등록된 원본(7921)을 찾기 위해 IN 절을 사용합니다.
     */
    fun findByCardCompanyAndCardLastFourDigitsIn(
        cardCompany: CardCompany,
        cardLastFourDigits: List<String>
    ): CardLimit?
}
