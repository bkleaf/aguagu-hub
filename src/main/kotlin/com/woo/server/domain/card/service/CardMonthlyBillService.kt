package com.woo.server.domain.card.service

import com.woo.server.common.enums.CardCompany
import com.woo.server.common.notification.TelegramNotificationService
import com.woo.server.domain.card.dto.CardMonthlyBillListResponse
import com.woo.server.domain.card.dto.CardMonthlyBillProcessResponse
import com.woo.server.domain.card.dto.CardMonthlyBillResponse
import com.woo.server.domain.card.entity.CardMonthlyBill
import com.woo.server.domain.card.parser.BillParserFactory
import com.woo.server.domain.card.repository.CardMonthlyBillRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 월별 청구서 서비스
 *
 * 카드사 월별 청구 문자의 수신, 파싱, 저장 및 조회 기능을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
class CardMonthlyBillService(
    private val cardMonthlyBillRepository: CardMonthlyBillRepository,
    private val billParserFactory: BillParserFactory,
    private val telegramNotificationService: TelegramNotificationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 청구서 문자를 처리합니다.
     *
     * 1. 청구서 문자 파싱
     * 2. 중복 확인
     * 3. DB 저장
     * 4. Telegram 알림 전송
     *
     * @param phoneNumber 발신 전화번호
     * @param cardCompany 카드사
     * @param message 청구서 문자 메시지
     * @return 처리 결과
     */
    @Transactional
    fun processBillMessage(
        phoneNumber: String,
        cardCompany: CardCompany,
        message: String
    ): CardMonthlyBillProcessResponse {
        log.info("청구서 문자 처리 시작 - 카드사: ${cardCompany.displayName}, 발신번호: $phoneNumber")

        // 청구서 파싱
        val parseResult = billParserFactory.parse(cardCompany, message)

        if (!parseResult.success) {
            log.warn("청구서 파싱 실패: ${parseResult.failReason}")
            return CardMonthlyBillProcessResponse.parseFailed(parseResult.failReason ?: "알 수 없는 오류")
        }

        // 중복 확인
        val isDuplicate = cardMonthlyBillRepository.existsByCardCompanyAndBillingDateAndReferenceDate(
            cardCompany = cardCompany,
            billingDate = parseResult.billingDate!!,
            referenceDate = parseResult.referenceDate!!
        )

        if (isDuplicate) {
            log.warn("중복 청구서 감지됨")
            return CardMonthlyBillProcessResponse.duplicate()
        }

        // 저장
        val bill = CardMonthlyBill(
            phoneNumber = phoneNumber,
            cardCompany = cardCompany,
            billingDate = parseResult.billingDate,
            billingAmount = parseResult.billingAmount!!,
            referenceDate = parseResult.referenceDate,
            rawMessage = message
        )

        val saved = cardMonthlyBillRepository.save(bill)
        log.info("청구서 저장 완료: ID=${saved.id}")

        // Telegram 알림
        try {
            telegramNotificationService.notifyMonthlyBill(saved)
        } catch (e: Exception) {
            log.error("텔레그램 알림 실패", e)
        }

        return CardMonthlyBillProcessResponse.success(CardMonthlyBillResponse.from(saved))
    }

    /**
     * 월별 청구서를 조회합니다 (합계 포함).
     *
     * @param year 년도
     * @param month 월
     * @param cardCompany 카드사 (null이면 전체)
     * @return 청구서 목록 + 합계
     */
    fun getBillsByMonth(year: Int, month: Int, cardCompany: CardCompany? = null): CardMonthlyBillListResponse {
        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.withDayOfMonth(startDate.lengthOfMonth())

        val bills = if (cardCompany != null) {
            cardMonthlyBillRepository.findByCardCompanyAndBillingDateBetweenOrderByBillingDateDesc(
                cardCompany, startDate, endDate
            )
        } else {
            cardMonthlyBillRepository.findByBillingDateBetweenOrderByBillingDateDesc(startDate, endDate)
        }

        val totalAmount = cardMonthlyBillRepository.sumBillingAmountByBillingDateBetween(startDate, endDate)

        return CardMonthlyBillListResponse(
            bills = bills.map { CardMonthlyBillResponse.from(it) },
            totalAmount = totalAmount,
            year = year,
            month = month
        )
    }

    /**
     * 전체 청구서를 조회합니다.
     *
     * @return 전체 청구서 목록
     */
    fun getAllBills(): List<CardMonthlyBillResponse> {
        return cardMonthlyBillRepository.findAllByOrderByBillingDateDesc()
            .map { CardMonthlyBillResponse.from(it) }
    }
}
