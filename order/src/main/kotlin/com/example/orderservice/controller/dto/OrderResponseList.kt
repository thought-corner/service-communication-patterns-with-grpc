package com.example.orderservice.controller.dto

import com.example.orderservice.service.dto.OrderResult

class OrderResponseList(
    var orders: List<OrderResponse> = emptyList()
) {
    companion object {
        fun from(orderResults: List<OrderResult>): OrderResponseList = OrderResponseList(orderResults.map { OrderResponse.from(it) })
    }
}
