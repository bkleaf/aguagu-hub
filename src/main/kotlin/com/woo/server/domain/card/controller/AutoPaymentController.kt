package com.woo.server.domain.card.controller

import com.woo.server.domain.card.dto.AutoPaymentRequest
import com.woo.server.domain.card.dto.AutoPaymentResponse
import com.woo.server.domain.card.service.AutoPaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 자동결제 관리 컨트롤러
 *
 * 카드별 정기 결제의 등록, 조회, 수정, 삭제 API를 제공합니다.
 */
@Tag(name = "자동결제", description = "자동결제 등록/조회/수정/삭제 API")
@RestController
@RequestMapping("/api/v1/card/auto-payments")
class AutoPaymentController(
    private val autoPaymentService: AutoPaymentService
) {

    /** 자동결제를 등록합니다. */
    @Operation(summary = "자동결제 등록", description = "신용카드에 연결된 자동결제를 등록합니다.")
    @PostMapping
    fun create(
        @Valid @RequestBody request: AutoPaymentRequest
    ): ResponseEntity<AutoPaymentResponse> {
        val result = autoPaymentService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    /** 전체 자동결제를 조회합니다. */
    @Operation(summary = "전체 자동결제 조회")
    @GetMapping
    fun getAll(): ResponseEntity<List<AutoPaymentResponse>> {
        return ResponseEntity.ok(autoPaymentService.getAll())
    }

    /** 단건 자동결제를 조회합니다. */
    @Operation(summary = "단건 자동결제 조회")
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<AutoPaymentResponse> {
        val result = autoPaymentService.getById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(result)
    }

    /** 특정 신용카드의 자동결제를 조회합니다. */
    @Operation(summary = "신용카드별 자동결제 조회")
    @GetMapping("/by-card/{creditCardId}")
    fun getByCreditCardId(@PathVariable creditCardId: String): ResponseEntity<List<AutoPaymentResponse>> {
        return ResponseEntity.ok(autoPaymentService.getByCreditCardId(creditCardId))
    }

    /** 자동결제를 수정합니다. */
    @Operation(summary = "자동결제 수정", description = "기존 자동결제 정보를 수정합니다.")
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: AutoPaymentRequest
    ): ResponseEntity<AutoPaymentResponse> {
        val result = autoPaymentService.update(id, request)
        return ResponseEntity.ok(result)
    }

    /** 자동결제를 삭제합니다. */
    @Operation(summary = "자동결제 삭제")
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        return if (autoPaymentService.delete(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
