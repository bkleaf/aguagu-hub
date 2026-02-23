package com.woo.server.domain.card.controller

import com.woo.server.domain.card.dto.SetMainTagRequest
import com.woo.server.domain.card.dto.TagRequest
import com.woo.server.domain.card.dto.TagResponse
import com.woo.server.domain.card.dto.TransactionTagRequest
import com.woo.server.domain.card.dto.TransactionTagResponse
import com.woo.server.domain.card.service.TagService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 태그 관리 컨트롤러
 *
 * 태그의 CRUD 및 거래-태그 할당/해제 API를 제공합니다.
 * 태그 유형(MAIN/DETAIL)에 따라 주요 태그와 세부 태그를 구분하여 관리합니다.
 */
@Tag(name = "태그", description = "태그 관리 및 거래-태그 할당/해제 API")
@RestController
@RequestMapping("/api/v1/card/tags")
class TagController(
    private val tagService: TagService
) {

    /** 태그를 생성합니다. */
    @Operation(summary = "태그 생성", description = "새로운 태그를 생성합니다.")
    @PostMapping
    fun create(
        @Valid @RequestBody request: TagRequest
    ): ResponseEntity<TagResponse> {
        val result = tagService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    /** 전체 태그를 조회합니다. */
    @Operation(summary = "전체 태그 조회")
    @GetMapping
    fun getAll(): ResponseEntity<List<TagResponse>> {
        return ResponseEntity.ok(tagService.getAll())
    }

    /** 태그를 수정합니다. */
    @Operation(summary = "태그 수정", description = "기존 태그의 이름과 색상을 수정합니다.")
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: TagRequest
    ): ResponseEntity<TagResponse> {
        val result = tagService.update(id, request)
        return ResponseEntity.ok(result)
    }

    /** 태그를 삭제합니다. */
    @Operation(summary = "태그 삭제")
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        return if (tagService.delete(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /** 거래에 태그를 할당합니다 (tagType: MAIN 또는 DETAIL). */
    @Operation(summary = "거래에 태그 할당", description = "거래에 태그를 할당합니다. tagType으로 MAIN/DETAIL 구분이 가능합니다.")
    @PostMapping("/transactions/{transactionId}/tags")
    fun addTagToTransaction(
        @PathVariable transactionId: Long,
        @RequestBody request: TransactionTagRequest
    ): ResponseEntity<TransactionTagResponse> {
        val result = tagService.addTagToTransaction(transactionId, request.tagId, request.tagType)
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    /** 거래의 주요 태그를 설정합니다. 기존 MAIN → DETAIL로 변경 후 새 태그를 MAIN으로 설정합니다. */
    @Operation(summary = "주요 태그 설정", description = "거래의 주요 태그를 설정/변경합니다. 기존 주요 태그는 세부 태그로 전환됩니다.")
    @PutMapping("/transactions/{transactionId}/main-tag")
    fun setMainTag(
        @PathVariable transactionId: Long,
        @RequestBody request: SetMainTagRequest
    ): ResponseEntity<TransactionTagResponse> {
        val result = tagService.setMainTag(transactionId, request.tagId)
        return ResponseEntity.ok(result)
    }

    /** 거래에서 태그를 해제합니다. */
    @Operation(summary = "거래에서 태그 해제")
    @DeleteMapping("/transactions/{transactionId}/tags/{tagId}")
    fun removeTagFromTransaction(
        @PathVariable transactionId: Long,
        @PathVariable tagId: Long
    ): ResponseEntity<Void> {
        tagService.removeTagFromTransaction(transactionId, tagId)
        return ResponseEntity.noContent().build()
    }

    /** 거래에 할당된 태그 목록을 조회합니다 (태그 유형 포함). */
    @Operation(summary = "거래의 태그 목록 조회", description = "거래에 할당된 태그 목록을 태그 유형(MAIN/DETAIL) 포함하여 조회합니다.")
    @GetMapping("/transactions/{transactionId}/tags")
    fun getTagsByTransaction(
        @PathVariable transactionId: Long
    ): ResponseEntity<List<TransactionTagResponse>> {
        return ResponseEntity.ok(tagService.getTagsByTransactionId(transactionId))
    }
}
