package com.example.orderservice.vo

import java.util.Date

class OrderItem(
	var productId: String? = null,
	var qty: Int? = null,
	var unitPrice: Int? = null,
	var totalPrice: Int? = null,
	var orderId: String? = null,
	var userId: String? = null,
	var createdAt: Date? = null,
	var updatedAt: Date? = null
)
