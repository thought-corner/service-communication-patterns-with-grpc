package com.example.userservice.service

import com.example.userservice.controller.dto.user.UserRequest
import com.example.userservice.entity.User
import com.example.userservice.repository.UserRepository
import com.example.userservice.service.dto.UserResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.modelmapper.ModelMapper
import org.modelmapper.convention.MatchingStrategies
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@ExtendWith(MockitoExtension::class)
class UserServiceImplTest {
	@Mock
	private lateinit var userRepository: UserRepository

	@Mock
	lateinit var passwordEncoder: BCryptPasswordEncoder

	@Spy
	private val modelMapper: ModelMapper = ModelMapper().apply {
		configuration.matchingStrategy = MatchingStrategies.STRICT
	}

	@InjectMocks
	private lateinit var userService: UserServiceImpl

	private lateinit var savedUser: UserResult

	@BeforeEach
	fun test_createUser() {
		val userRequest = UserRequest()
		userRequest.email = "edowon0623@gmail.com"
		userRequest.name = "Kenneth Lee"
		userRequest.pwd = "12345678"

		`when`(passwordEncoder.encode("12345678")).thenReturn("encodedPassword")
		`when`(userRepository.save(any())).thenAnswer { it.arguments[0] as User }

		savedUser = userService.createUser(userRequest)
	}

	@Test
	fun test_getUserByUserId() {
	}
}
