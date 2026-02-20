package com.woo.server.domain.card.controller

import com.woo.server.common.enums.CardCompany
import com.woo.server.domain.card.dto.CardMessageProcessResponse
import com.woo.server.domain.card.dto.CardTransactionResponse
import com.woo.server.domain.card.service.CardTransactionService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDateTime

@WebMvcTest(CardTransactionController::class)
class CardTransactionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var cardTransactionService: CardTransactionService

    private val baseUrl = "/api/v1/card/transactions"

    private fun sampleTransaction(
        id: Long = 1L,
        cardCompany: CardCompany = CardCompany.SAMSUNG,
        amount: BigDecimal = BigDecimal("15000.00"),
        merchantName: String = "스타벅스강남점",
        parseSuccess: Boolean = true
    ) = CardTransactionResponse(
        id = id,
        phoneNumber = "01012345678",
        cardCompany = cardCompany,
        cardCompanyName = cardCompany.displayName,
        amount = amount,
        transactionDate = LocalDateTime.of(2025, 1, 15, 14, 30),
        merchantName = merchantName,
        accumulatedAmount = BigDecimal("150000.00"),
        parseSuccess = parseSuccess,
        parseFailReason = if (!parseSuccess) "파싱 실패" else null,
        createdAt = LocalDateTime.now()
    )

    @Nested
    @DisplayName("POST /api/v1/card/transactions")
    inner class ReceiveCardMessage {

        @Test
        @DisplayName("삼성카드 결제 문자 등록 - 성공")
        fun `should process samsung card message successfully`() {
            val transaction = sampleTransaction()
            given(cardTransactionService.processCardMessage("01012345678", "[Web발신]\n삼성카드 승인\n홍*동\n15,000원 일시불\n01/15 14:30\n스타벅스강남점\n누적 150,000원"))
                .willReturn(CardMessageProcessResponse.success(transaction))

            mockMvc.perform(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "phoneNumber": "01012345678",
                          "message": "[Web발신]\n삼성카드 승인\n홍*동\n15,000원 일시불\n01/15 14:30\n스타벅스강남점\n누적 150,000원"
                        }
                    """.trimIndent())
            )
                .andDo(print())
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.transaction.cardCompany").value("SAMSUNG"))
        }

        @Test
        @DisplayName("파싱 실패 문자 등록")
        fun `should handle parse failure`() {
            val failedTx = sampleTransaction(parseSuccess = false)
            given(cardTransactionService.processCardMessage("01012345678", "알 수 없는 문자"))
                .willReturn(CardMessageProcessResponse.parseFailed(failedTx))

            mockMvc.perform(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "phoneNumber": "01012345678",
                          "message": "알 수 없는 문자"
                        }
                    """.trimIndent())
            )
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("필수 필드 누락 시 400 에러")
        fun `should return 400 when required fields missing`() {
            mockMvc.perform(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"phoneNumber": "", "message": ""}""")
            )
                .andDo(print())
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/card/transactions")
    inner class GetAllTransactions {

        @Test
        @DisplayName("전체 거래 내역 조회")
        fun `should return all transactions`() {
            given(cardTransactionService.getAllTransactions())
                .willReturn(listOf(sampleTransaction(1), sampleTransaction(2, merchantName = "이마트용산점")))

            mockMvc.perform(get(baseUrl).accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(2))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/card/transactions/{id}")
    inner class GetTransaction {

        @Test
        @DisplayName("특정 거래 조회 - 존재")
        fun `should return transaction by id`() {
            given(cardTransactionService.getTransaction(1L))
                .willReturn(sampleTransaction())

            mockMvc.perform(get("$baseUrl/1").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.merchantName").value("스타벅스강남점"))
        }

        @Test
        @DisplayName("특정 거래 조회 - 미존재 시 404")
        fun `should return 404 when not found`() {
            given(cardTransactionService.getTransaction(999L)).willReturn(null)

            mockMvc.perform(get("$baseUrl/999").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/card/transactions/by-company/{cardCompany}")
    inner class GetByCardCompany {

        @Test
        @DisplayName("카드사별 거래 조회 - SAMSUNG")
        fun `should return transactions by card company`() {
            given(cardTransactionService.getTransactionsByCardCompany(CardCompany.SAMSUNG))
                .willReturn(listOf(sampleTransaction()))

            mockMvc.perform(get("$baseUrl/by-company/SAMSUNG").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[0].cardCompany").value("SAMSUNG"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/card/transactions/failed & /successful")
    inner class GetByParseStatus {

        @Test
        @DisplayName("파싱 실패 거래 조회")
        fun `should return failed transactions`() {
            given(cardTransactionService.getFailedTransactions())
                .willReturn(listOf(sampleTransaction(parseSuccess = false)))

            mockMvc.perform(get("$baseUrl/failed").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[0].parseSuccess").value(false))
        }

        @Test
        @DisplayName("파싱 성공 거래 조회")
        fun `should return successful transactions`() {
            given(cardTransactionService.getSuccessfulTransactions())
                .willReturn(listOf(sampleTransaction()))

            mockMvc.perform(get("$baseUrl/successful").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[0].parseSuccess").value(true))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/card/transactions/supported-companies")
    inner class GetSupportedCompanies {

        @Test
        @DisplayName("지원 카드사 목록 조회")
        fun `should return supported card companies`() {
            given(cardTransactionService.getSupportedCardCompanies())
                .willReturn(listOf(CardCompany.SAMSUNG))

            mockMvc.perform(get("$baseUrl/supported-companies").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[0].code").value("SAMSUNG"))
                .andExpect(jsonPath("$[0].name").value("삼성카드"))
        }
    }
}
