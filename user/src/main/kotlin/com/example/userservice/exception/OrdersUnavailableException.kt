package com.example.userservice.exception

/**
 * 필수(essential) 주문 조회가 재시도 소진/서킷 OPEN으로 끝내 실패했을 때 던진다.
 * GlobalExceptionHandler가 이를 HTTP 503 + Retry-After 헤더로 매핑한다.
 */
class OrdersUnavailableException(
    val retryAfterSeconds: Long = 5,
    message: String = "order-service is temporarily unavailable",
    cause: Throwable? = null
) : RuntimeException(message, cause)
