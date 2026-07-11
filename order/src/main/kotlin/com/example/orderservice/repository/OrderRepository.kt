package com.example.orderservice.repository

import com.example.orderservice.entity.Order
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long> {

	fun findByOrderId(orderId: String): Order?

	fun findByUserId(userId: String): Iterable<Order>
}