package com.example.userservice.controller.dto.order

import java.time.LocalDateTime

class OrderResponse(
	var orderId: String? = null,
	var productId: String? = null,
	var qty: Int? = null,
	var unitPrice: Int? = null,
	var totalPrice: Int? = null,
	var createdAt: LocalDateTime? = null,
	var updatedAt: LocalDateTime? = null
)