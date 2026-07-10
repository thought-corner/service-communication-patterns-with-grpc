package com.example.userservice.controller.dto.order

class OrderResponse(
	var orderId: String? = null,
	var productId: String? = null,
	var qty: Int? = null,
	var unitPrice: Int? = null,
	var totalPrice: Int? = null,
	var createdAt: String? = null,
	var updatedAt: String? = null
)