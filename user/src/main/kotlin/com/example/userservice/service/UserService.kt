package com.example.userservice.service

import com.example.userservice.controller.dto.user.UserRequest
import com.example.userservice.service.dto.UserResult

interface UserService {

    fun createUser(userRequest: UserRequest): UserResult

    fun getUserByUserId(userId: String): UserResult

    fun getUserByAll(): List<UserResult>

    fun getUserDetailsByEmail(email: String): UserResult
}
