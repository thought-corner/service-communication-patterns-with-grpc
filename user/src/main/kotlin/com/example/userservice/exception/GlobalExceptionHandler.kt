package com.example.userservice.exception

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleInvalidRequest(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, String?>> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to it.defaultMessage }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors)
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(ex: BusinessException): ResponseEntity<Map<String, String?>> {
        val errorCode = ex.errorCode
        return ResponseEntity.status(errorCode.status)
            .body(mapOf("code" to errorCode.name, "message" to ex.message))
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(ex: DataIntegrityViolationException): ResponseEntity<Map<String, String?>> {
        val errorCode = ErrorCode.EMAIL_ALREADY_EXISTS
        return ResponseEntity.status(errorCode.status)
            .body(mapOf("code" to errorCode.name, "message" to errorCode.message))
    }

    @ExceptionHandler(OrdersUnavailableException::class)
    fun handleOrdersUnavailable(ex: OrdersUnavailableException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header(HttpHeaders.RETRY_AFTER, ex.retryAfterSeconds.toString())
            .body(mapOf("code" to "ORDERS_UNAVAILABLE", "message" to ex.message))
}