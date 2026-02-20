package com.woo.server.domain.card.controller

import com.woo.server.domain.card.dto.CardLimitRequest
import com.woo.server.domain.card.dto.CardLimitResponse
import com.woo.server.domain.card.service.CardLimitService
import io.swagger.v3.oas.annotations.Operation
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

    @Operation(summary = "전체 한도 조회")
    @GetMapping
    fun getAll(): ResponseEntity<List<CardLimitResponse>> {
        return ResponseEntity.ok(cardLimitService.getAll())
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
