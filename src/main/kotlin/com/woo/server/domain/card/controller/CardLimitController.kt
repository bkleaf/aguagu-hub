package com.woo.server.domain.card.controller

import com.woo.server.domain.card.dto.CardLimitRequest
import com.woo.server.domain.card.dto.CardLimitResponse
import com.woo.server.domain.card.service.CardLimitService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "카드 한도", description = "카드 월간 한도 설정 및 조회 API")
@RestController
@RequestMapping("/api/v1/card/limits")
class CardLimitController(
    private val cardLimitService: CardLimitService
) {

    @Operation(summary = "카드 한도 생성/수정", description = "카드사+끝4자리 조합으로 한도를 생성하거나 수정합니다.")
    @PostMapping
    fun createOrUpdate(
        @Valid @RequestBody request: CardLimitRequest
    ): ResponseEntity<CardLimitResponse> {
        val result = cardLimitService.createOrUpdate(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @Operation(summary = "전체 한도 조회", description = "year, month를 지정하면 해당 년/월 기준 사용액을 계산합니다. 미지정 시 현재 기준.")
    @GetMapping
    fun getAll(
        @Parameter(description = "조회 기준 년도 (예: 2026)")
        @RequestParam year: Int?,
        @Parameter(description = "조회 기준 월 (1~12)")
        @RequestParam month: Int?
    ): ResponseEntity<List<CardLimitResponse>> {
        // month 파라미터 범위 검증 (1~12)
        if (month != null) {
            require(month in 1..12) { "월은 1~12 사이의 값이어야 합니다: $month" }
        }
        val result = if (year != null && month != null) {
            cardLimitService.getAllByYearMonth(year, month)
        } else {
            cardLimitService.getAll()
        }
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "단건 한도 조회")
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<CardLimitResponse> {
        val result = cardLimitService.getById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "한도 삭제")
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        return if (cardLimitService.delete(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
