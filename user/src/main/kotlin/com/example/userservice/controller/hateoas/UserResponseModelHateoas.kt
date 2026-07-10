package com.example.userservice.controller.hateoas

import com.example.userservice.controller.UserController
import com.example.userservice.controller.dto.order.OrderResponse
import com.example.userservice.controller.dto.user.UserResponse
import com.example.userservice.service.dto.UserResult
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.stereotype.Component

@Component
class UserResponseModelHateoas {

	fun toModel(userResult: UserResult, orders: List<OrderResponse>? = null): EntityModel<UserResponse> {
		val entityModel = EntityModel.of(UserResponse.of(userResult, orders))
		entityModel.add(linkTo(methodOn(UserController::class.java).getUsers()).withRel("all-users"))
		return entityModel
	}
}
