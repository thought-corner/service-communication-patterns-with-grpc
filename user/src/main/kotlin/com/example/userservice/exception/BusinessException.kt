package com.example.userservice.exception

class BusinessException(
    val errorCode: ErrorCode,
    detail: String? = null,
) : RuntimeException(detail ?: errorCode.message)
