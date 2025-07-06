package io.hhplus.tdd

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

data class ErrorResponse(val code: String, val message: String)

@RestControllerAdvice
class ApiControllerAdvice : ResponseEntityExceptionHandler() {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(InvalidUserIdException::class)
    fun handleInvalidUserIdException(e: InvalidUserIdException): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid user ID: ${e.message}")
        return ResponseEntity(
            ErrorResponse("INVALID_USER_ID", e.message ?: "유효하지 않은 사용자 ID입니다."),
            HttpStatus.BAD_REQUEST,
        )
    }
    
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred", e)
        return ResponseEntity(
            ErrorResponse("500", "에러가 발생했습니다."),
            HttpStatus.INTERNAL_SERVER_ERROR,
        )
    }
}