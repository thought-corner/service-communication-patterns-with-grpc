package com.example.userservice.service.dto

import com.example.userservice.controller.dto.order.OrderResponse

enum class OrdersStatus { OK, UNAVAILABLE }

class OrdersResult(
    val status: OrdersStatus,
    val orders: List<OrderResponse>?
) {
    companion object {
        fun ok(orders: List<OrderResponse>): OrdersResult = OrdersResult(OrdersStatus.OK, orders)
        fun unavailable(): OrdersResult = OrdersResult(OrdersStatus.UNAVAILABLE, null)
    }
}
