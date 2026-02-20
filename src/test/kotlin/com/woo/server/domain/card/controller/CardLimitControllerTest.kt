package com.woo.server.domain.card.controller

import com.woo.server.common.enums.CardCompany
import com.woo.server.common.enums.LimitType
import com.woo.server.domain.card.dto.CardLimitResponse
import com.woo.server.domain.card.service.CardLimitService
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

@WebMvcTest(CardLimitController::class)
class CardLimitControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var cardLimitService: CardLimitService

    private val baseUrl = "/api/v1/card/limits"

    private fun sampleLimit(
        id: Long = 1L,
        cardCompany: CardCompany = CardCompany.SAMSUNG,
        digits: String = "9684",
        limitType: LimitType = LimitType.MONTHLY,
        limitAmount: BigDecimal = BigDecimal("1000000"),
        usedAmount: BigDecimal = BigDecimal("150000")
    ) = CardLimitResponse(
        id = id,
        creditCardId = "${cardCompany.name}_$digits",
        cardCompany = cardCompany,
        cardCompanyName = cardCompany.displayName,
        cardLastFourDigits = digits,
        limitType = limitType,
        limitTypeName = limitType.displayName,
        limitAmount = limitAmount,
        usedAmount = usedAmount,
        remaining = limitAmount - usedAmount,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @Nested
    @DisplayName("POST /api/v1/card/limits")
    inner class CreateOrUpdate {

        @Test
        @DisplayName("카드 한도 설정 - 월간 신규 생성")
        fun `should create card limit`() {
            given(cardLimitService.createOrUpdate(org.mockito.kotlin.any()))
                .willReturn(sampleLimit())

            mockMvc.perform(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "creditCardId": "SAMSUNG_9684",
                          "limitType": "MONTHLY",
                          "limitAmount": 1000000
                        }
                    """.trimIndent())
            )
                .andDo(print())
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.cardCompany").value("SAMSUNG"))
                .andExpect(jsonPath("$.cardLastFourDigits").value("9684"))
                .andExpect(jsonPath("$.creditCardId").value("SAMSUNG_9684"))
                .andExpect(jsonPath("$.limitType").value("MONTHLY"))
                .andExpect(jsonPath("$.limitAmount").value(1000000))
        }

        @Test
        @DisplayName("필수 필드 누락 시 400 에러")
        fun `should return 400 when required fields missing`() {
            mockMvc.perform(
                post(baseUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"limitAmount": 1000000}""")
            )
                .andDo(print())
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/card/limits")
    inner class GetAll {

        @Test
        @DisplayName("전체 한도 조회")
        fun `should return all limits`() {
            given(cardLimitService.getAll())
                .willReturn(listOf(sampleLimit(1), sampleLimit(2, digits = "1234")))

            mockMvc.perform(get(baseUrl).accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(2))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/card/limits/{id}")
    inner class GetById {

        @Test
        @DisplayName("단건 한도 조회 - 존재")
        fun `should return limit by id`() {
            given(cardLimitService.getById(1L)).willReturn(sampleLimit())

            mockMvc.perform(get("$baseUrl/1").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.remaining").value(850000))
        }

        @Test
        @DisplayName("단건 한도 조회 - 미존재 시 404")
        fun `should return 404 when not found`() {
            given(cardLimitService.getById(999L)).willReturn(null)

            mockMvc.perform(get("$baseUrl/999").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/card/limits/{id}")
    inner class Delete {

        @Test
        @DisplayName("한도 삭제 - 성공")
        fun `should delete limit`() {
            given(cardLimitService.delete(1L)).willReturn(true)

            mockMvc.perform(delete("$baseUrl/1"))
                .andDo(print())
                .andExpect(status().isNoContent)
        }

        @Test
        @DisplayName("한도 삭제 - 미존재 시 404")
        fun `should return 404 when deleting non-existent`() {
            given(cardLimitService.delete(999L)).willReturn(false)

            mockMvc.perform(delete("$baseUrl/999"))
                .andDo(print())
                .andExpect(status().isNotFound)
        }
    }
}
