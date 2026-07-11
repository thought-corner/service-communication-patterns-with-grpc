package com.example.userservice.vo

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

class LoginCredentials(
    @field:NotNull(message = "Email cannot be null")
    @field:Size(min = 2, message = "Email not be less than two characters")
    @field:Email
    var email: String? = null,

    @field:NotNull(message = "Password cannot be null")
    @field:Size(min = 8, message = "Password must be equals or grater than 8 characters")
    var password: String? = null
)
