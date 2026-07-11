package com.example.orderservice.controller.dto

import com.example.orderservice.service.dto.OrderResult
import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
class OrderResponse(
    var productId: String? = null,
    var qty: Int? = null,
    var unitPrice: Int? = null,
    var totalPrice: Int? = null,
    var createdAt: LocalDateTime? = null,
    var updatedAt: LocalDateTime? = null,
    var orderId: String? = null
) {
    companion object {
        fun from(orderResult: OrderResult): OrderResponse = OrderResponse(
            productId = orderResult.productId,
            qty = orderResult.qty,
            unitPrice = orderResult.unitPrice,
            totalPrice = orderResult.totalPrice,
            createdAt = orderResult.createdAt,
            updatedAt = orderResult.updatedAt,
            orderId = orderResult.orderId
        )
    }
}