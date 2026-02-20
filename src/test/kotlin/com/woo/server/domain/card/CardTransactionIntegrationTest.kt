package com.woo.server.domain.card

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * 카드 결제 문자 수신 → DB 저장 → Telegram 알림 전체 흐름 통합 테스트
 *
 * 실제 DB와 Telegram Bot에 연동하여 테스트합니다.
 * application.properties 설정이 필요합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CardTransactionIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val baseUrl = "/api/v1/card/transactions"

    @Test
    @DisplayName("삼성카드 결제 문자 수신 → 파싱 → DB 저장 → Telegram 알림 전체 흐름")
    fun `full flow - receive samsung card sms, parse, save to db, send telegram`() {
        // 1. 삼성카드 결제 문자 전송
        val result = mockMvc.perform(
            post(baseUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "phoneNumber": "01012345678",
                      "message": "삼성9684승인\n전*우\n4,500원 일시불\n01/15 17:52\n버거킹건대입구역\n누적773,774"
                    }
                """.trimIndent())
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.transaction.cardCompany").value("SAMSUNG"))
            .andExpect(jsonPath("$.transaction.amount").value(4500.00))
            .andExpect(jsonPath("$.transaction.merchantName").value("버거킹건대입구역"))
            .andExpect(jsonPath("$.transaction.parseSuccess").value(true))
            .andReturn()

        // 2. 저장된 거래 ID 추출
        val responseBody = result.response.contentAsString
        val idMatch = Regex(""""id"\s*:\s*(\d+)""").find(responseBody)
        val savedId = idMatch!!.groupValues[1]

        // 3. 저장된 거래를 DB에서 다시 조회하여 검증
        mockMvc.perform(
            get("$baseUrl/$savedId")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(savedId.toLong()))
            .andExpect(jsonPath("$.phoneNumber").value("01012345678"))
            .andExpect(jsonPath("$.cardCompany").value("SAMSUNG"))
            .andExpect(jsonPath("$.cardCompanyName").value("삼성카드"))
            .andExpect(jsonPath("$.amount").value(4500.00))
            .andExpect(jsonPath("$.merchantName").value("버거킹건대입구역"))
            .andExpect(jsonPath("$.accumulatedAmount").value(773774))
            .andExpect(jsonPath("$.parseSuccess").value(true))

        // 4. 동일 문자 재전송 → 중복 거래 처리 확인
        mockMvc.perform(
            post(baseUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "phoneNumber": "01012345678",
                      "message": "삼성9684승인\n전*우\n4,500원 일시불\n01/15 17:52\n버거킹건대입구역\n누적773,774"
                    }
                """.trimIndent())
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("이미 동일한 거래가 존재합니다"))

        // 5. 파싱 실패 케이스
        mockMvc.perform(
            post(baseUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "phoneNumber": "01012345678",
                      "message": "택배 도착 알림: 문 앞에 놓았습니다"
                    }
                """.trimIndent())
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.transaction.parseSuccess").value(false))

        // 6. 파싱 성공 목록에 포함 확인
        mockMvc.perform(
            get("$baseUrl/successful")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.merchantName == '버거킹건대입구역')]").exists())

        // 7. 카드사별 조회
        mockMvc.perform(
            get("$baseUrl/by-company/SAMSUNG")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.merchantName == '버거킹건대입구역')]").exists())
    }
}
