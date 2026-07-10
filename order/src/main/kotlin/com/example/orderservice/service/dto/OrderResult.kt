package com.example.orderservice.service.dto

import com.example.orderservice.vo.OrderItem
import java.util.Date

class OrderResult(
	var productId: String? = null,
	var qty: Int? = null,
	var unitPrice: Int? = null,
	var totalPrice: Int? = null,
	var orderId: String? = null,
	var userId: String? = null,
	var createdAt: Date? = null,
	var updatedAt: Date? = null
) {
	companion object {
		fun from(orderItem: OrderItem): OrderResult = OrderResult(
			productId = orderItem.productId,
			qty = orderItem.qty,
			unitPrice = orderItem.unitPrice,
			totalPrice = orderItem.totalPrice,
			orderId = orderItem.orderId,
			userId = orderItem.userId,
			createdAt = orderItem.createdAt,
			updatedAt = orderItem.updatedAt
		)
	}
}
