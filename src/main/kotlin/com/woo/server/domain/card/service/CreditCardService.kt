package com.woo.server.domain.card.service

import com.woo.server.common.enums.CardCompany
import com.woo.server.domain.card.dto.CreditCardRequest
import com.woo.server.domain.card.dto.CreditCardResponse
import com.woo.server.domain.card.entity.CreditCard
import com.woo.server.domain.card.repository.CreditCardRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 신용카드 관리 서비스
 *
 * 신용카드의 등록, 조회, 삭제 기능을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
class CreditCardService(
    private val creditCardRepository: CreditCardRepository
) {

    /**
     * 신용카드를 등록합니다.
     * 이미 등록된 카드사+끝4자리 조합이면 정산 기간을 업데이트합니다.
     */
    @Transactional
    fun register(request: CreditCardRequest): CreditCardResponse {
        val id = CreditCard.generateId(request.cardCompany, request.lastFourDigits)
        val existing = creditCardRepository.findById(id).orElse(null)

        val creditCard = if (existing != null) {
            existing.billingStartDay = request.billingStartDay
            existing.billingEndDay = request.billingEndDay
            existing.paymentDay = request.paymentDay
            existing.active = request.active
            existing.updatedAt = LocalDateTime.now()
            creditCardRepository.save(existing)
        } else {
            creditCardRepository.save(
                CreditCard(
                    id = id,
                    cardCompany = request.cardCompany,
                    lastFourDigits = request.lastFourDigits,
                    billingStartDay = request.billingStartDay,
                    billingEndDay = request.billingEndDay,
                    paymentDay = request.paymentDay,
                    active = request.active
                )
            )
        }

        return CreditCardResponse.from(creditCard)
    }

    /** 전체 신용카드를 조회합니다. */
    fun getAll(): List<CreditCardResponse> {
        return creditCardRepository.findAll().map { CreditCardResponse.from(it) }
    }

    /** 단건 신용카드를 조회합니다. */
    fun getById(id: String): CreditCardResponse? {
        return creditCardRepository.findById(id).orElse(null)?.let { CreditCardResponse.from(it) }
    }

    /** 신용카드를 삭제합니다. */
    @Transactional
    fun delete(id: String): Boolean {
        if (!creditCardRepository.existsById(id)) return false
        creditCardRepository.deleteById(id)
        return true
    }

    /**
     * 카드사와 끝4자리(마스킹 변형 포함)로 등록된 신용카드를 찾습니다.
     *
     * 마스킹된 값(7*2*)으로도 원본(7921)으로 등록된 카드를 매칭합니다.
     */
    fun findByCardInfo(cardCompany: CardCompany, cardLastFourDigits: String): CreditCard? {
        // 직접 매칭
        val direct = creditCardRepository.findByCardCompanyAndLastFourDigits(cardCompany, cardLastFourDigits)
        if (direct != null) return direct

        // 마스킹 변형 매칭 (숫자 4자리 → 마스킹 형태로 검색)
        val variants = buildCardDigitVariants(cardLastFourDigits)
        if (variants.size > 1) {
            val matches = creditCardRepository.findByCardCompanyAndLastFourDigitsIn(cardCompany, variants)
            if (matches.isNotEmpty()) return matches.first()
        }

        // 마스킹 패턴 역매칭 (예: "7*2*" → LIKE "7_2_"로 DB 검색)
        val likePattern = buildLikePattern(cardLastFourDigits)
        if (likePattern != null) {
            val matches = creditCardRepository.findByCardCompanyAndLastFourDigitsLike(cardCompany, likePattern)
            if (matches.isNotEmpty()) return matches.first()
        }

        return null
    }

    /**
     * 카드 끝자리의 가능한 변형 목록을 생성합니다.
     */
    private fun buildCardDigitVariants(digits: String): List<String> {
        val variants = mutableListOf(digits)
        if (digits.length == 4 && digits.all { it.isDigit() }) {
            // 숫자 4자리 → 하나카드 마스킹 변형 추가
            val masked = "${digits[0]}*${digits[2]}*"
            variants.add(masked)
        }
        return variants
    }

    /**
     * 마스킹된 카드번호를 LIKE 패턴으로 변환합니다.
     * 예: "7*2*" → "7_2_" (SQL LIKE 와일드카드)
     */
    private fun buildLikePattern(digits: String): String? {
        if (digits.length != 4 || !digits.contains('*')) return null
        return digits.replace('*', '_')
    }
}
