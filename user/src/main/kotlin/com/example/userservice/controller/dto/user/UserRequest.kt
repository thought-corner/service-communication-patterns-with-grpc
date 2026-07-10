package com.example.userservice.controller.dto.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotNull

class UserRequest(
	@field:NotNull(message = "email cannot be null")
	@field:Email
	var email: String? = null,

	@field:NotNull(message = "name cannot be null")
	var name: String? = null,

	@field:NotNull(message = "pwd cannot be null")
	var pwd: String? = null
)
