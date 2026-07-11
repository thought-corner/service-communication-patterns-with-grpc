package com.example.userservice.service

import com.example.userservice.entity.User
import com.example.userservice.exception.BusinessException
import com.example.userservice.exception.ErrorCode
import com.example.userservice.repository.UserRepository
import com.example.userservice.controller.dto.user.UserRequest
import com.example.userservice.service.dto.UserResult
import com.example.userservice.vo.UserCredentials
import org.modelmapper.ModelMapper
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
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
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND, "User not found: $userId") }
        return UserResult.from(modelMapper.map(userEntity, UserCredentials::class.java))
    }

    override fun getUserByAll(): List<UserResult> {
        return userRepository.findAll().map { UserResult.from(modelMapper.map(it, UserCredentials::class.java)) }
    }

    override fun getUserDetailsByEmail(email: String): UserResult {
        val userEntity = userRepository.findByEmail(email)
            .orElseThrow { IllegalStateException("Authenticated user not found: $email") }
        return UserResult.from(modelMapper.map(userEntity, UserCredentials::class.java))
    }
}
