package com.example.userservice.repository

import com.example.userservice.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {

	fun findByUserId(userId: String): Optional<User>

	fun findByEmail(username: String): Optional<User>
}