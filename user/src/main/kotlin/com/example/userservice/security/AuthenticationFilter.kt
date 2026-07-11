package com.example.userservice.security

import com.example.userservice.service.UserService
import com.example.userservice.vo.LoginCredentials
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Validator
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import tools.jackson.databind.ObjectMapper

class AuthenticationFilter(
    authenticationManager: AuthenticationManager,
    private val userService: UserService,
    private val tokenProvider: JwtTokenProvider,
    private val validator: Validator,
    private val objectMapper: ObjectMapper
) : UsernamePasswordAuthenticationFilter(authenticationManager) {

    override fun attemptAuthentication(req: HttpServletRequest, res: HttpServletResponse): Authentication {
        val creds = objectMapper.readValue(req.inputStream, LoginCredentials::class.java)

        val violations = validator.validate(creds)
        if (violations.isNotEmpty()) {
            throw BadCredentialsException(violations.joinToString(", ") { it.message })
        }

        return authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(creds.email!!, creds.password!!, ArrayList<GrantedAuthority>())
        )
    }

    override fun successfulAuthentication(
        req: HttpServletRequest,
        res: HttpServletResponse,
        chain: FilterChain,
        auth: Authentication
    ) {
        val userName = (auth.principal as User).username
        val userDetails = userService.getUserDetailsByEmail(userName)

        val token = tokenProvider.generate(userDetails.userId!!)

        res.addHeader(SecurityConstants.TOKEN_HEADER, token)
        res.addHeader(SecurityConstants.USER_ID_HEADER, userDetails.userId)
    }
}
