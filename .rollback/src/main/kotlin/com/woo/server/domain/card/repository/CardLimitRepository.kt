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
}
