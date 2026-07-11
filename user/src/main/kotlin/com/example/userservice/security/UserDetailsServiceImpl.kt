package com.example.userservice.security

import com.example.userservice.repository.UserRepository
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User as SpringSecurityUser
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(
	private val userRepository: UserRepository
) : UserDetailsService {

	override fun loadUserByUsername(username: String): UserDetails {
		val userEntity = userRepository.findByEmail(username)
			.orElseThrow { UsernameNotFoundException("$username: not found") }

		return SpringSecurityUser(
			userEntity.email!!, userEntity.password!!,
			true, true, true, true,
			ArrayList<GrantedAuthority>()
		)
	}
}
