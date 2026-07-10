package com.example.orderservice.controller.dto

import jakarta.validation.constraints.NotNull

class OrderRequest(
	@field:NotNull(message = "productId cannot be null")
	var productId: String? = null,

	@field:NotNull(message = "qty cannot be null")
	var qty: Int? = null,

	@field:NotNull(message = "unitPrice cannot be null")
	var unitPrice: Int? = null
)
