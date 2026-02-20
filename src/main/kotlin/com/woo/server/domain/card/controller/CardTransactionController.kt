package com.woo.server.domain.card.controller

import com.woo.server.common.enums.CardCompany
import com.woo.server.domain.card.dto.CardMessageProcessResponse
import com.woo.server.domain.card.dto.CardMessageRequest
import com.woo.server.domain.card.dto.CardParseFailureResponse
import com.woo.server.domain.card.dto.CardTransactionResponse
import com.woo.server.domain.card.service.CardTransactionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "카드 거래", description = "카드 결제 문자 수신 및 거래 내역 조회 API")
@RestController
@RequestMapping("/api/v1/card/transactions")
class CardTransactionController(
    private val cardTransactionService: CardTransactionService
) {

    @Operation(summary = "카드 결제 문자 등록", description = "카드 결제 문자 메시지를 파싱하여 거래 정보를 추출하고 DB에 저장합니다.")
    @PostMapping
    fun receiveCardMeansssage(
        @Valid @RequestBody request: CardMessageRequest
    ): ResponseEntity<CardMessageProcessResponse> {
        val result = cardTransactionService.processCardMessage(request.phoneNumber, request.message)

        val status = if (result.success) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status).body(result)
    }

    @Operation(summary = "전체 거래 내역 조회")
    @GetMapping
    fun getAllTransactions(): ResponseEntity<List<CardTransactionResponse>> {
        val transactions = cardTransactionService.getAllTransactions()
        return ResponseEntity.ok(transactions)
    }

    @Operation(summary = "특정 거래 조회", description = "ID로 거래 내역을 조회합니다.")
    @GetMapping("/{id}")
    fun getTransaction(@PathVariable id: Long): ResponseEntity<CardTransactionResponse> {
        val transaction = cardTransactionService.getTransaction(id)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(transaction)
    }

    @Operation(summary = "파싱 실패 내역 조회", description = "파싱에 실패한 문자 내역을 조회합니다.")
    @GetMapping("/parse-failures")
    fun getParseFailures(): ResponseEntity<List<CardParseFailureResponse>> {
        val failures = cardTransactionService.getParseFailures()
        return ResponseEntity.ok(failures)
    }

    @Operation(summary = "거래 내역 조회 (거래일시순)", description = "거래일시 기준 최근순으로 조회합니다.")
    @GetMapping("/by-date")
    fun getTransactionsByDate(): ResponseEntity<List<CardTransactionResponse>> {
        val transactions = cardTransactionService.getTransactionsByDate()
        return ResponseEntity.ok(transactions)
    }

    @Operation(summary = "카드사별 거래 조회", description = "카드사 코드(예: SAMSUNG, HYUNDAI)로 거래 내역을 조회합니다.")
    @GetMapping("/by-company/{cardCompany}")
    fun getTransactionsByCardCompany(
        @PathVariable cardCompany: CardCompany
    ): ResponseEntity<List<CardTransactionResponse>> {
        val transactions = cardTransactionService.getTransactionsByCardCompany(cardCompany)
        return ResponseEntity.ok(transactions)
    }

    @Operation(summary = "지원 카드사 목록 조회")
    @GetMapping("/supported-companies")
    fun getSupportedCardCompanies(): ResponseEntity<List<Map<String, String>>> {
        val companies = cardTransactionService.getSupportedCardCompanies()
            .map { mapOf("code" to it.name, "name" to it.displayName, "phoneNumber" to it.phoneNumber, "billingPhoneNumber" to it.billingPhoneNumber) }
        return ResponseEntity.ok(companies)
    }
}
