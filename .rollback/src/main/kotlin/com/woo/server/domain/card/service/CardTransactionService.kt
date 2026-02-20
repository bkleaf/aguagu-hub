package com.woo.server.domain.card.service

import com.woo.server.common.enums.CardCompany
import com.woo.server.domain.card.dto.CardMessageProcessResponse
import com.woo.server.domain.card.dto.CardTransactionResponse
import com.woo.server.domain.card.entity.CardTransaction
import com.woo.server.domain.card.parser.CardParserFactory
import com.woo.server.domain.card.repository.CardTransactionRepository
import com.woo.server.common.notification.TelegramNotificationService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 카드 거래 내역 서비스
 *
 * 카드 결제 문자 메시지의 수신, 파싱, 저장 및 조회 기능을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
class CardTransactionService(
    private val cardTransactionRepository: CardTransactionRepository,
    private val cardParserFactory: CardParserFactory,
    private val telegramNotificationService: TelegramNotificationService,
    private val cardLimitService: CardLimitService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 카드 결제 문자 메시지를 처리합니다.
     *
     * 1. 카드사 자동 감지
     * 2. 문자 메시지 파싱
     * 3. 중복 거래 확인
     * 4. DB 저장
     *
     * @param message 카드 결제 문자 메시지
     * @return 처리 결과
     */
    @Transactional
    fun processCardMessage(phoneNumber: String, message: String): CardMessageProcessResponse {
        log.info("카드 문자 메시지 처리 시작 - 발신번호: $phoneNumber")

        // 카드사 감지
        val cardCompany = cardParserFactory.detectCardCompany(message)
        log.info("감지된 카드사: ${cardCompany.displayName}")

        // 문자 파싱
        val parseResult = cardParserFactory.parse(message)

        // 파싱 실패 시 실패 거래로 저장
        if (!parseResult.success) {
            log.warn("문자 파싱 실패: ${parseResult.failReason}")
            val failedTransaction = CardTransaction.createFailedTransaction(
                phoneNumber = phoneNumber,
                rawMessage = message,
                cardCompany = cardCompany,
                failReason = parseResult.failReason ?: "알 수 없는 오류"
            )
            val saved = cardTransactionRepository.save(failedTransaction)
            try { telegramNotificationService.notifyTransaction(saved) } catch (e: Exception) { log.error("텔레그램 알림 실패", e) }
            return CardMessageProcessResponse.parseFailed(CardTransactionResponse.from(saved))
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

        // 거래 저장
        val transaction = CardTransaction(
            phoneNumber = phoneNumber,
            cardCompany = cardCompany,
            cardLastFourDigits = parseResult.cardLastFourDigits,
            amount = parseResult.amount,
            transactionDate = parseResult.transactionDate,
            merchantName = parseResult.merchantName,
            accumulatedAmount = parseResult.accumulatedAmount,
            rawMessage = message,
            parseSuccess = true
        )

        val saved = cardTransactionRepository.save(transaction)
        log.info("거래 저장 완료: ID=${saved.id}")

        // 한도 정보 조회 후 알림 전송
        try {
            val limitInfo = if (parseResult.cardLastFourDigits != null) {
                cardLimitService.getLimitInfo(cardCompany, parseResult.cardLastFourDigits)
            } else null
            telegramNotificationService.notifyTransaction(saved, limitInfo)
        } catch (e: Exception) {
            log.error("텔레그램 알림 실패", e)
        }

        return CardMessageProcessResponse.success(CardTransactionResponse.from(saved))
    }

    /**
     * 모든 거래 내역을 조회합니다.
     *
     * @return 전체 거래 내역 목록 (최신순)
     */
    fun getAllTransactions(): List<CardTransactionResponse> {
        return cardTransactionRepository.findAllByOrderByCreatedAtDesc()
            .map { CardTransactionResponse.from(it) }
    }

    /**
     * 특정 거래 내역을 조회합니다.
     *
     * @param id 거래 ID
     * @return 거래 내역, 없으면 null
     */
    fun getTransaction(id: Long): CardTransactionResponse? {
        return cardTransactionRepository.findById(id)
            .map { CardTransactionResponse.from(it) }
            .orElse(null)
    }

    /**
     * 파싱 실패한 거래 내역만 조회합니다.
     *
     * @return 파싱 실패 거래 내역 목록
     */
    fun getFailedTransactions(): List<CardTransactionResponse> {
        return cardTransactionRepository.findByParseSuccessFalse()
            .map { CardTransactionResponse.from(it) }
    }

    /**
     * 파싱 성공한 거래 내역만 조회합니다.
     *
     * @return 파싱 성공 거래 내역 목록 (거래일시 내림차순)
     */
    fun getSuccessfulTransactions(): List<CardTransactionResponse> {
        return cardTransactionRepository.findByParseSuccessTrueOrderByTransactionDateDesc()
            .map { CardTransactionResponse.from(it) }
    }

    /**
     * 카드사별 거래 내역을 조회합니다.
     *
     * @param cardCompany 카드사
     * @return 해당 카드사의 거래 내역 목록
     */
    fun getTransactionsByCardCompany(cardCompany: CardCompany): List<CardTransactionResponse> {
        return cardTransactionRepository.findByCardCompany(cardCompany)
            .map { CardTransactionResponse.from(it) }
    }

    /**
     * 현재 지원하는 카드사 목록을 조회합니다.
     *
     * @return 지원 카드사 목록
     */
    fun getSupportedCardCompanies(): List<CardCompany> {
        return cardParserFactory.getSupportedCardCompanies()
    }
}
