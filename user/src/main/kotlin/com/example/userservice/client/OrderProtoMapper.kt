package com.example.userservice.client

import com.example.order.v1.Order
import com.example.userservice.controller.dto.order.OrderResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * order.v1.Order(proto) → OrderResponse(app DTO) 매핑.
 * 타임스탬프는 ISO-8601(ISO_LOCAL_DATE_TIME) 문자열이며, 빈 문자열은 null로 정규화한다.
 */
object OrderProtoMapper {

    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun toResponse(order: Order): OrderResponse = OrderResponse(
        orderId = order.orderId.ifBlank { null },
        productId = order.productId.ifBlank { null },
        qty = order.qty,
        unitPrice = order.unitPrice,
        totalPrice = order.totalPrice,
        createdAt = order.createdAt.toLocalDateTimeOrNull(),
        updatedAt = order.updatedAt.toLocalDateTimeOrNull()
    )

    private fun String.toLocalDateTimeOrNull(): LocalDateTime? =
        if (isBlank()) null else LocalDateTime.parse(this, ISO)
}
