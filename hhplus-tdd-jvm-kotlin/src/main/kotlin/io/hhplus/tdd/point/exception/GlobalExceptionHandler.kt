package io.hhplus.tdd.point.exception

import io.hhplus.tdd.InvalidUserIdException
import io.hhplus.tdd.InvalidAmountException
import io.hhplus.tdd.InsufficientPointException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(InvalidUserIdException::class)
    fun handleInvalidUserIdException(e: InvalidUserIdException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("INVALID_USER_ID", e.message ?: "유효하지 않은 사용자 ID입니다"))
    }

    @ExceptionHandler(InvalidAmountException::class)
    fun handleInvalidAmountException(e: InvalidAmountException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("INVALID_AMOUNT", e.message ?: "유효하지 않은 금액입니다"))
    }

    @ExceptionHandler(InsufficientPointException::class)
    fun handleInsufficientPointException(e: InsufficientPointException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("INSUFFICIENT_POINT", e.message ?: "잔액이 부족합니다"))
    }
}

data class ErrorResponse(
    val code: String,
    val message: String
)
