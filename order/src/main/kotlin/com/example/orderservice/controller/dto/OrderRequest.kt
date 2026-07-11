package com.example.orderservice.controller.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

class OrderRequest(
	@field:NotNull(message = "productId cannot be null")
	var productId: String? = null,

	@field:NotNull(message = "qty cannot be null")
	@field:Positive(message = "qty must be greater than zero")
	var qty: Int? = null,

	@field:NotNull(message = "unitPrice cannot be null")
	@field:Positive(message = "unitPrice must be greater than zero")
	var unitPrice: Int? = null
)
