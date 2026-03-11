package com.woo.server.common.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.DateTimeException

/**
 * 글로벌 예외 핸들러
 *
 * 비즈니스 예외를 적절한 HTTP 상태 코드와 일관된 JSON 형식으로 응답합니다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    /** 잘못된 인자 예외 → 400 Bad Request */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        log.warn("잘못된 요청: ${e.message}")
        return ResponseEntity.badRequest().body(
            ErrorResponse(error = e.message ?: "잘못된 요청입니다", status = 400)
        )
    }

    /** 상태 충돌 예외 → 409 Conflict */
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(e: IllegalStateException): ResponseEntity<ErrorResponse> {
        log.warn("상태 충돌: ${e.message}")
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse(error = e.message ?: "상태 충돌이 발생했습니다", status = 409)
        )
    }

    /** 리소스 미발견 예외 → 404 Not Found */
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElement(e: NoSuchElementException): ResponseEntity<ErrorResponse> {
        log.warn("리소스 미발견: ${e.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(error = e.message ?: "리소스를 찾을 수 없습니다", status = 404)
        )
    }

    /** Bean Validation 실패 → 400 Bad Request */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = e.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        log.warn("유효성 검증 실패: $message")
        return ResponseEntity.badRequest().body(
            ErrorResponse(error = message, status = 400)
        )
    }

    /** 잘못된 날짜 파라미터 → 400 Bad Request */
    @ExceptionHandler(DateTimeException::class)
    fun handleDateTime(e: DateTimeException): ResponseEntity<ErrorResponse> {
        log.warn("날짜 형식 오류: ${e.message}")
        return ResponseEntity.badRequest().body(
            ErrorResponse(error = e.message ?: "잘못된 날짜 형식입니다", status = 400)
        )
    }

    /** 기타 예외 → 500 Internal Server Error */
    @ExceptionHandler(Exception::class)
    fun handleGeneral(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("서버 내부 오류", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(error = "서버 내부 오류가 발생했습니다", status = 500)
        )
    }
}

/** 에러 응답 DTO */
data class ErrorResponse(
    val error: String,
    val status: Int
)
