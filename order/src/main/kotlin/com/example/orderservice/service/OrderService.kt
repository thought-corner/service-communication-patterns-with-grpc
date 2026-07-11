package com.example.orderservice.service

import com.example.orderservice.controller.dto.OrderRequest
import com.example.orderservice.service.dto.OrderResult

interface OrderService {

    fun createOrder(orderRequest: OrderRequest, userId: String): OrderResult

    fun getOrderByOrderId(orderId: String): OrderResult?

    fun getOrdersByUserId(userId: String): List<OrderResult>
}
