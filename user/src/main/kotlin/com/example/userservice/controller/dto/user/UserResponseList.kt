package com.example.userservice.controller.dto.user

import com.example.userservice.service.dto.UserResult

class UserResponseList(
	var users: List<UserResponse> = emptyList()
) {
	companion object {
		fun from(userResults: List<UserResult>): UserResponseList = UserResponseList(userResults.map { UserResponse.of(it) })
	}
}
