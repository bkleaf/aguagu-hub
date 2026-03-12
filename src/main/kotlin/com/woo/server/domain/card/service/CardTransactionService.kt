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
import java.time.LocalDate

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

        // 거래 저장 시 사용할 카드 끝4자리 (매칭된 CreditCard가 있으면 원본 끝4자리 사용)
        val cardLastFourDigits = matchedCreditCard?.lastFourDigits ?: parseResult.cardLastFourDigits

        // 취소 문자 처리
        if (parseResult.cancelled) {
            return processCancellation(cardCompany, cardLastFourDigits, parseResult, phoneNumber, message)
        }

        // 중복 거래 확인 (카드 끝4자리 포함)
        val isDuplicate = cardTransactionRepository.existsByCardCompanyAndCardLastFourDigitsAndAmountAndTransactionDateAndMerchantName(
            cardCompany = cardCompany,
            cardLastFourDigits = cardLastFourDigits,
            amount = parseResult.amount!!,
            transactionDate = parseResult.transactionDate!!,
            merchantName = parseResult.merchantName!!
        )

        if (isDuplicate) {
            log.warn("중복 거래 감지됨")
            return CardMessageProcessResponse.duplicate()
        }

        // 거래 저장
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

    /**
     * 취소 문자를 처리합니다.
     *
     * 1. 원본 거래 검색 (카드사 + 카드 끝4자리 + 금액 + 사용처 + 취소되지 않은 건)
     * 2. 매칭 성공: 원본 거래의 cancelled를 true로 설정
     * 3. 매칭 실패: 취소 거래를 cancelled=true로 새로 저장
     */
    private fun processCancellation(
        cardCompany: CardCompany,
        cardLastFourDigits: String?,
        parseResult: com.woo.server.domain.card.parser.ParseResult,
        phoneNumber: String,
        message: String
    ): CardMessageProcessResponse {
        log.info("취소 문자 처리 시작 - 카드사: ${cardCompany.displayName}, 금액: ${parseResult.amount}")

        // 원본 거래 검색
        val originalTransaction = if (cardLastFourDigits != null) {
            cardTransactionRepository.findFirstByCardCompanyAndCardLastFourDigitsAndAmountAndMerchantNameAndCancelledFalseOrderByTransactionDateDesc(
                cardCompany = cardCompany,
                cardLastFourDigits = cardLastFourDigits,
                amount = parseResult.amount!!,
                merchantName = parseResult.merchantName!!
            )
        } else null

        if (originalTransaction != null) {
            // 매칭 성공: 원본 거래 취소 표시
            originalTransaction.cancelled = true
            val saved = cardTransactionRepository.save(originalTransaction)
            log.info("원본 거래 취소 처리 완료: ID=${saved.id}")

            try {
                telegramNotificationService.notifyCancellation(saved)
            } catch (e: Exception) {
                log.error("텔레그램 취소 알림 실패", e)
            }

            return CardMessageProcessResponse.cancelSuccess(CardTransactionResponse.from(saved))
        } else {
            // 매칭 실패: 취소 거래를 cancelled=true로 새로 저장
            log.warn("원본 거래를 찾을 수 없음 - 취소 거래 별도 저장")
            val cancelTransaction = CardTransaction(
                phoneNumber = phoneNumber,
                cardCompany = cardCompany,
                cardLastFourDigits = cardLastFourDigits,
                amount = parseResult.amount!!,
                transactionDate = parseResult.transactionDate!!,
                merchantName = parseResult.merchantName!!,
                accumulatedAmount = null,
                rawMessage = message,
                cancelled = true
            )

            val saved = cardTransactionRepository.save(cancelTransaction)
            log.info("취소 거래 별도 저장 완료: ID=${saved.id}")

            try {
                telegramNotificationService.notifyCancellation(saved)
            } catch (e: Exception) {
                log.error("텔레그램 취소 알림 실패", e)
            }

            return CardMessageProcessResponse.cancelUnmatched(CardTransactionResponse.from(saved))
        }
    }

    /** 모든 거래 내역을 조회합니다 (태그 포함). */
    fun getAllTransactions(): List<CardTransactionResponse> {
        val transactions = cardTransactionRepository.findAllByOrderByCreatedAtDesc()
        val tagMap = getTagMapForTransactions(transactions)
        return transactions.map { tx ->
            CardTransactionResponse.from(tx, tagMap[tx.id] ?: emptyList())
        }
    }

    /**
     * 기간별 거래 내역을 조회합니다 (태그 포함).
     *
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 해당 기간의 거래 내역 목록 (거래일시 내림차순)
     */
    fun getTransactionsByDateRange(startDate: LocalDate, endDate: LocalDate): List<CardTransactionResponse> {
        val startDateTime = startDate.atStartOfDay()
        val endDateTime = endDate.atTime(23, 59, 59, 999_999_999)
        val transactions = cardTransactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(
            startDateTime, endDateTime
        )
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

    /** 현재 지원하는 카드사 목록을 조회합니다 (거래 파서 + 청구서 파서 포함). */
    fun getSupportedCardCompanies(): List<CardCompany> {
        val transactionCompanies = cardParserFactory.getSupportedCardCompanies()
        val billCompanies = CardCompany.entries
            .filter { it.billingPhoneNumber.isNotEmpty() }
        return (transactionCompanies + billCompanies).distinct()
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
