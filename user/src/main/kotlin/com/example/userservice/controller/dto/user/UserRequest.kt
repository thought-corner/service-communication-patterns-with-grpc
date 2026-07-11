package com.example.userservice.controller.dto.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

class UserRequest(
	@field:NotBlank(message = "email must not be blank")
	@field:Email
	var email: String? = null,

	@field:NotBlank(message = "name must not be blank")
	var name: String? = null,

	@field:NotBlank(message = "pwd must not be blank")
	var pwd: String? = null
)
