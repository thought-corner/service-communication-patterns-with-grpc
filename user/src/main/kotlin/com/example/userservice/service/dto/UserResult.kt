package com.example.userservice.service.dto

import com.example.userservice.vo.UserCredentials

class UserResult(
    var email: String? = null,
    var name: String? = null,
    var userId: String? = null,
    var password: String? = null
) {
    companion object {
        fun from(userCredentials: UserCredentials): UserResult = UserResult(
            email = userCredentials.email,
            name = userCredentials.name,
            userId = userCredentials.userId,
            password = userCredentials.password
        )
    }
}
