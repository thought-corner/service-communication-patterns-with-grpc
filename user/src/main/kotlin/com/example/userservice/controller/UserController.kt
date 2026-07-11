package com.example.userservice.controller

import com.example.userservice.client.OrderServiceClient
import com.example.userservice.controller.dto.user.UserRequest
import com.example.userservice.service.UserService
import com.example.userservice.controller.dto.user.UserResponse
import com.example.userservice.controller.dto.user.UserResponseList
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
    private val orderServiceClient: OrderServiceClient
) {

    @PostMapping
    fun createUser(@Valid @RequestBody user: UserRequest): ResponseEntity<UserResponse> {
        val createdUser = userService.createUser(user)
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.of(createdUser))
    }

    @GetMapping
    fun getUsers(): ResponseEntity<UserResponseList> {
        val userList = userService.getUserByAll()
        return ResponseEntity.status(HttpStatus.OK).body(UserResponseList.from(userList))
    }

    @GetMapping("/me")
    fun getMyProfile(@AuthenticationPrincipal userId: String): ResponseEntity<UserResponse> {
        val user = userService.getUserByUserId(userId)
        val ordersResult = orderServiceClient.getOrders(userId)
        return ResponseEntity.status(HttpStatus.OK).body(UserResponse.of(user, ordersResult))
    }
}
