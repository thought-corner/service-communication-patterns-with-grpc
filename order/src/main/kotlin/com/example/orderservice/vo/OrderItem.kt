package com.example.orderservice.vo

import java.time.LocalDateTime

class OrderItem(
	var productId: String? = null,
	var qty: Int? = null,
	var unitPrice: Int? = null,
	var totalPrice: Long? = null,
	var orderId: String? = null,
	var userId: String? = null,
	var createdAt: LocalDateTime? = null,
	var updatedAt: LocalDateTime? = null
)
