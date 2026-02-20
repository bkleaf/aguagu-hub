package com.woo.server.domain.card.repository

import com.woo.server.common.enums.CardCompany
import com.woo.server.domain.card.entity.CardParseFailure
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 카드 문자 파싱 실패 내역 리포지토리
 */
@Repository
interface CardParseFailureRepository : JpaRepository<CardParseFailure, Long> {

    /** 최근 생성순으로 전체 조회 */
    fun findAllByOrderByCreatedAtDesc(): List<CardParseFailure>

    /** 카드사별 조회 */
    fun findByCardCompanyOrderByCreatedAtDesc(cardCompany: CardCompany): List<CardParseFailure>
}
