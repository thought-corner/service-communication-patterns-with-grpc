package com.example.userservice.service

import com.example.userservice.entity.User
import com.example.userservice.repository.UserRepository
import com.example.userservice.controller.dto.user.UserRequest
import com.example.userservice.controller.dto.order.OrderResponse
import com.example.userservice.controller.dto.order.OrderResponseList
import com.example.userservice.service.dto.UserResult
import com.example.userservice.vo.UserCredentials
import io.github.oshai.kotlinlogging.KotlinLogging
import org.modelmapper.ModelMapper
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory
import org.springframework.http.HttpMethod
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val restTemplate: RestTemplate?,
    private val circuitBreakerFactory: CircuitBreakerFactory<*, *>?,
    private val modelMapper: ModelMapper
) : UserService {

    @Transactional
    override fun createUser(userRequest: UserRequest): UserResult {
        val userCredentials = UserCredentials(
            email = userRequest.email,
            name = userRequest.name
        )
        userCredentials.userId = UUID.randomUUID().toString()
        userCredentials.password = passwordEncoder.encode(userRequest.pwd!!)

        val user = modelMapper.map(userCredentials, User::class.java)
        val savedUser = userRepository.save(user)
        return UserResult.from(modelMapper.map(savedUser, UserCredentials::class.java))
    }

    override fun getUserByUserId(userId: String): UserResult {
        val userEntity = userRepository.findByUserId(userId)
            .orElseThrow { UsernameNotFoundException("User not found") }
        return UserResult.from(modelMapper.map(userEntity, UserCredentials::class.java))
    }

    override fun getOrdersByUserId(userId: String): List<OrderResponse> {
        val circuitBreaker = circuitBreakerFactory!!.create("circuitBreaker1")

        return circuitBreaker.run(
            {
                log.info { "Before call orders microservice" }
                val orderUrl = "http://127.0.0.1:8082/orders/$userId"
                val orderListResponse = restTemplate!!.exchange(
                    orderUrl, HttpMethod.GET, null,
                    OrderResponseList::class.java
                )
                log.info { "After called orders microservice using restful api" }

                orderListResponse.body?.orders ?: emptyList()
            },
            { throwable ->
                log.error(throwable) { "Failed to call orders microservice, returning empty list" }
                emptyList()
            }
        )
    }

    override fun getUserByAll(): List<UserResult> {
        return userRepository.findAll().map { UserResult.from(modelMapper.map(it, UserCredentials::class.java)) }
    }

    override fun getUserDetailsByEmail(email: String): UserResult {
        val userEntity = userRepository.findByEmail(email)
            .orElseThrow { UsernameNotFoundException(email) }
        return UserResult.from(modelMapper.map(userEntity, UserCredentials::class.java))
    }
}
