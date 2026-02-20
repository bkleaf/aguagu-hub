package com.woo.server.domain.card.controller

import com.woo.server.domain.card.dto.CreditCardRequest
import com.woo.server.domain.card.dto.CreditCardResponse
import com.woo.server.domain.card.dto.UsageSummaryOverview
import com.woo.server.domain.card.service.CardUsageSummaryService
import com.woo.server.domain.card.service.CreditCardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 신용카드 관리 컨트롤러
 *
 * 신용카드의 등록, 조회, 삭제 API를 제공합니다.
 */
@Tag(name = "신용카드", description = "신용카드 등록/조회/삭제 API")
@RestController
@RequestMapping("/api/v1/card/credit-cards")
class CreditCardController(
    private val creditCardService: CreditCardService,
    private val cardUsageSummaryService: CardUsageSummaryService
) {

    /** 신용카드를 등록합니다. */
    @Operation(summary = "신용카드 등록", description = "카드사+끝4자리로 신용카드를 등록합니다. 이미 존재하면 정산 기간을 업데이트합니다.")
    @PostMapping
    fun register(
        @Valid @RequestBody request: CreditCardRequest
    ): ResponseEntity<CreditCardResponse> {
        val result = creditCardService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    /** 카드별 사용 현황 요약을 조회합니다. */
    @Operation(summary = "카드별 사용 현황 요약 조회", description = "활성 카드의 정산기간 기준 사용액, 한도 대비 사용률을 조회합니다.")
    @GetMapping("/usage-summary")
    fun getUsageSummary(): ResponseEntity<UsageSummaryOverview> {
        return ResponseEntity.ok(cardUsageSummaryService.getUsageSummary())
    }

    /** 전체 신용카드를 조회합니다. */
    @Operation(summary = "전체 신용카드 조회")
    @GetMapping
    fun getAll(): ResponseEntity<List<CreditCardResponse>> {
        return ResponseEntity.ok(creditCardService.getAll())
    }

    /** 단건 신용카드를 조회합니다. */
    @Operation(summary = "단건 신용카드 조회")
    @GetMapping("/{id}")
    fun getById(@PathVariable id: String): ResponseEntity<CreditCardResponse> {
        val result = creditCardService.getById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(result)
    }

    /** 신용카드를 삭제합니다. */
    @Operation(summary = "신용카드 삭제")
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: String): ResponseEntity<Void> {
        return if (creditCardService.delete(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
