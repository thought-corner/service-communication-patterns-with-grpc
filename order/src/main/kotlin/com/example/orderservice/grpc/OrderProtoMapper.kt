package com.example.orderservice.grpc

import com.example.order.v1.Order
import com.example.orderservice.service.dto.OrderResult
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * OrderResult(도메인) → order.v1.Order(proto) 매핑.
 * 타임스탬프는 ISO-8601(ISO_LOCAL_DATE_TIME) 문자열로 인코딩하고, null은 빈 문자열로 내보낸다.
 */
object OrderProtoMapper {

    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun toProto(order: OrderResult): Order = Order.newBuilder()
        .setOrderId(order.orderId ?: "")
        .setProductId(order.productId ?: "")
        .setQty(order.qty ?: 0)
        .setUnitPrice(order.unitPrice ?: 0)
        .setTotalPrice(order.totalPrice ?: 0L)
        .setUserId(order.userId ?: "")
        .setCreatedAt(order.createdAt.toIso())
        .setUpdatedAt(order.updatedAt.toIso())
        .build()

    private fun LocalDateTime?.toIso(): String = this?.format(ISO) ?: ""
}
