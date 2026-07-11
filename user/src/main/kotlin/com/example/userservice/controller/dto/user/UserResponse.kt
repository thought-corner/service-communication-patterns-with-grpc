package com.example.userservice.controller.dto.user

import com.example.userservice.controller.dto.order.OrderResponse
import com.example.userservice.service.dto.OrdersResult
import com.example.userservice.service.dto.UserResult
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
class UserResponse(
	var email: String? = null,
	var name: String? = null,
	var userId: String? = null,
	var orders: List<OrderResponse>? = null,
	var ordersStatus: String? = null
) {
	companion object {
		fun of(userResult: UserResult, ordersResult: OrdersResult? = null): UserResponse = UserResponse(
			email = userResult.email,
			name = userResult.name,
			userId = userResult.userId,
			orders = ordersResult?.orders,
			ordersStatus = ordersResult?.status?.name
		)
	}
}