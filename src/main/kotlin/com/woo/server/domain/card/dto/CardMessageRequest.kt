package com.woo.server.domain.card.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "카드 결제 문자 메시지 수신 요청")
data class CardMessageRequest(
    @Schema(description = "발신자 전화번호", example = "01012345678")
    @field:NotBlank(message = "전화번호는 필수입니다")
    val phoneNumber: String,

    @Schema(
        description = "카드 결제 문자 메시지 내용",
        example = "삼성9684승인\n전*우\n4,500원 일시불\n01/15 17:52\n버거킹건대입구역\n누적773,774"
    )
    @field:NotBlank(message = "문자 메시지는 필수입니다")
    val message: String
)
