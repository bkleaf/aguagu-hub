package com.woo.server.domain.card.service

import com.woo.server.common.enums.CardCompany
import com.woo.server.domain.card.dto.CardMessageProcessResponse
import com.woo.server.domain.card.dto.CardMonthlyBillProcessResponse
import com.woo.server.domain.card.dto.CardParseFailureResponse
import com.woo.server.domain.card.dto.CardTransactionResponse
import com.woo.server.domain.card.dto.TransactionTagResponse
import com.woo.server.domain.card.entity.CardParseFailure
import com.woo.server.domain.card.entity.CardTransaction
import com.woo.server.domain.card.parser.CardParserFactory
import com.woo.server.domain.card.repository.CardParseFailureRepository
import com.woo.server.domain.card.repository.CardTransactionRepository
import com.woo.server.domain.card.repository.TransactionTagRepository
import com.woo.server.common.notification.TelegramNotificationService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 카드 거래 내역 서비스
 *
 * 카드 결제 문자 메시지의 수신, 파싱, 저장 및 조회 기능을 제공합니다.
 * 파싱 성공 시 CardTransaction에, 실패 시 CardParseFailure에 저장합니다.
 */
@Service
@Transactional(readOnly = true)
class CardTransactionService(
    private val cardTransactionRepository: CardTransactionRepository,
    private val cardParseFailureRepository: CardParseFailureRepository,
    private val cardParserFactory: CardParserFactory,
    private val telegramNotificationService: TelegramNotificationService,
    private val cardLimitService: CardLimitService,
    private val creditCardService: CreditCardService,
    private val cardMonthlyBillService: CardMonthlyBillService,
    private val transactionTagRepository: TransactionTagRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 카드 결제 문자 메시지를 처리합니다.
     *
     * 1. 카드사 자동 감지
     * 2. 문자 메시지 파싱
     * 3. 등록된 CreditCard와 매칭
     * 4. 중복 거래 확인
     * 5. DB 저장
     * 6. Telegram 알림 전송
     */
    @Transactional
    fun processCardMessage(phoneNumber: String, message: String): CardMessageProcessResponse {
        log.info("카드 문자 메시지 처리 시작 - 발신번호: $phoneNumber")

        // 발신번호 기반 청구서 감지 → 청구서 처리로 위임
        val billingCompany = CardCompany.detectBillingCompany(phoneNumber)
        if (billingCompany != null) {
            log.info("청구서 발신번호 감지됨: ${billingCompany.displayName} ($phoneNumber)")
            val billResult = cardMonthlyBillService.processBillMessage(phoneNumber, billingCompany, message)
            return CardMessageProcessResponse.billProcessed(billResult)
        }

        // 카드사 감지
        val cardCompany = cardParserFactory.detectCardCompany(message)
        log.info("감지된 카드사: ${cardCompany.displayName}")

        // 문자 파싱
        val parseResult = cardParserFactory.parse(message)

        // 파싱 실패 시 실패 테이블에 저장
        if (!parseResult.success) {
            log.warn("문자 파싱 실패: ${parseResult.failReason}")
            val failure = CardParseFailure(
                phoneNumber = phoneNumber,
                cardCompany = cardCompany,
                rawMessage = message,
                failReason = parseResult.failReason ?: "알 수 없는 오류"
            )
            val saved = cardParseFailureRepository.save(failure)
            try { telegramNotificationService.notifyParseFailure(saved) } catch (e: Exception) { log.error("텔레그램 알림 실패", e) }
            return CardMessageProcessResponse.parseFailed(CardParseFailureResponse.from(saved))
        }

        // 등록된 CreditCard와 매칭 시도
        val matchedCreditCard = if (parseResult.cardLastFourDigits != null) {
            creditCardService.findByCardInfo(cardCompany, parseResult.cardLastFourDigits)
        } else null

        if (matchedCreditCard != null) {
            log.info("등록된 신용카드 매칭됨: ${matchedCreditCard.id}")
        } else {
            log.info("등록된 신용카드 없음 (미등록 카드)")
        }

        // 중복 거래 확인
        val isDuplicate = cardTransactionRepository.existsByCardCompanyAndAmountAndTransactionDateAndMerchantName(
            cardCompany = cardCompany,
            amount = parseResult.amount!!,
            transactionDate = parseResult.transactionDate!!,
            merchantName = parseResult.merchantName!!
        )

        if (isDuplicate) {
            log.warn("중복 거래 감지됨")
            return CardMessageProcessResponse.duplicate()
        }

        // 거래 저장 (매칭된 CreditCard가 있으면 원본 끝4자리 사용)
        val cardLastFourDigits = matchedCreditCard?.lastFourDigits ?: parseResult.cardLastFourDigits
        val transaction = CardTransaction(
            phoneNumber = phoneNumber,
            cardCompany = cardCompany,
            cardLastFourDigits = cardLastFourDigits,
            amount = parseResult.amount,
            transactionDate = parseResult.transactionDate,
            merchantName = parseResult.merchantName,
            accumulatedAmount = parseResult.accumulatedAmount,
            rawMessage = message
        )

        val saved = cardTransactionRepository.save(transaction)
        log.info("거래 저장 완료: ID=${saved.id}")

        // 한도 정보 조회 후 알림 전송 (한도 미설정 시 월간 사용액만 조회)
        try {
            val limitInfo = if (cardLastFourDigits != null) {
                cardLimitService.getLimitInfo(cardCompany, cardLastFourDigits)
            } else null
            val monthlyUsage = if (limitInfo == null && cardLastFourDigits != null) {
                cardLimitService.getMonthlyUsage(cardCompany, cardLastFourDigits)
            } else null
            telegramNotificationService.notifyTransaction(saved, limitInfo, monthlyUsage)
        } catch (e: Exception) {
            log.error("텔레그램 알림 실패", e)
        }

        return CardMessageProcessResponse.success(CardTransactionResponse.from(saved))
    }

    /** 모든 거래 내역을 조회합니다 (태그 포함). */
    fun getAllTransactions(): List<CardTransactionResponse> {
        val transactions = cardTransactionRepository.findAllByOrderByCreatedAtDesc()
        val tagMap = getTagMapForTransactions(transactions)
        return transactions.map { tx ->
            CardTransactionResponse.from(tx, tagMap[tx.id] ?: emptyList())
        }
    }

    /** 특정 거래 내역을 조회합니다 (태그 포함). */
    fun getTransaction(id: Long): CardTransactionResponse? {
        return cardTransactionRepository.findById(id)
            .map { tx ->
                val tags = transactionTagRepository.findByTransactionIdWithTag(tx.id!!)
                    .map { TransactionTagResponse.from(it) }
                CardTransactionResponse.from(tx, tags)
            }
            .orElse(null)
    }

    /** 파싱 실패 내역을 조회합니다. */
    fun getParseFailures(): List<CardParseFailureResponse> {
        return cardParseFailureRepository.findAllByOrderByCreatedAtDesc()
            .map { CardParseFailureResponse.from(it) }
    }

    /** 거래 내역을 거래일시 기준 최근순으로 조회합니다 (태그 포함). */
    fun getTransactionsByDate(): List<CardTransactionResponse> {
        val transactions = cardTransactionRepository.findAllByOrderByTransactionDateDesc()
        val tagMap = getTagMapForTransactions(transactions)
        return transactions.map { tx ->
            CardTransactionResponse.from(tx, tagMap[tx.id] ?: emptyList())
        }
    }

    /** 카드사별 거래 내역을 조회합니다 (태그 포함). */
    fun getTransactionsByCardCompany(cardCompany: CardCompany): List<CardTransactionResponse> {
        val transactions = cardTransactionRepository.findByCardCompany(cardCompany)
        val tagMap = getTagMapForTransactions(transactions)
        return transactions.map { tx ->
            CardTransactionResponse.from(tx, tagMap[tx.id] ?: emptyList())
        }
    }

    /** 현재 지원하는 카드사 목록을 조회합니다. */
    fun getSupportedCardCompanies(): List<CardCompany> {
        return cardParserFactory.getSupportedCardCompanies()
    }

    /**
     * 거래 목록에 대한 태그 맵을 일괄 조회합니다 (N+1 방지).
     * 거래 ID → 태그 응답 리스트 맵을 반환합니다 (태그 유형 포함).
     */
    private fun getTagMapForTransactions(transactions: List<CardTransaction>): Map<Long?, List<TransactionTagResponse>> {
        val transactionIds = transactions.mapNotNull { it.id }
        if (transactionIds.isEmpty()) return emptyMap()
        return transactionTagRepository.findByTransactionIdInWithTag(transactionIds)
            .groupBy { it.transaction.id }
            .mapValues { (_, tags) -> tags.map { TransactionTagResponse.from(it) } }
    }
}
