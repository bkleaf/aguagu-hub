package com.woo.server.domain.card.dto

import com.woo.server.domain.card.entity.Tag
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 태그 생성/수정 요청 DTO
 */
@Schema(description = "태그 생성/수정 요청")
data class TagRequest(
    @Schema(description = "태그 이름", example = "식비")
    @field:NotBlank(message = "태그 이름은 필수입니다")
    @field:Size(max = 50, message = "태그 이름은 50자 이내여야 합니다")
    val name: String,

    @Schema(description = "태그 색상 (hex)", example = "#2196F3")
    @field:NotBlank(message = "태그 색상은 필수입니다")
    @field:Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "올바른 hex 색상 형식이어야 합니다 (예: #2196F3)")
    val color: String = "#2196F3"
)

/**
 * 태그 응답 DTO
 */
@Schema(description = "태그 응답")
data class TagResponse(
    @Schema(description = "태그 ID", example = "1")
    val id: Long,

    @Schema(description = "태그 이름", example = "식비")
    val name: String,

    @Schema(description = "태그 색상 (hex)", example = "#2196F3")
    val color: String,

    @Schema(description = "생성 일시", example = "2026-02-20T10:00:00")
    val createdAt: LocalDateTime
) {
    companion object {
        /**
         * Entity를 Response DTO로 변환
         *
         * @param entity Tag 엔티티
         * @return TagResponse DTO
         */
        fun from(entity: Tag): TagResponse {
            return TagResponse(
                id = entity.id!!,
                name = entity.name,
                color = entity.color,
                createdAt = entity.createdAt
            )
        }
    }
}

/**
 * 거래에 태그 할당 요청 DTO
 */
@Schema(description = "거래에 태그 할당 요청")
data class TransactionTagRequest(
    @Schema(description = "태그 ID", example = "1")
    val tagId: Long
)
