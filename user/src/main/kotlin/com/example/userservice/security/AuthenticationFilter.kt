package com.example.userservice.security

import com.example.userservice.service.UserService
import com.example.userservice.vo.LoginCredentials
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
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
import java.time.Instant
import java.util.Base64
import java.util.Date

class AuthenticationFilter(
    authenticationManager: AuthenticationManager,
    private val userService: UserService,
    private val tokenSecret: String,
    private val tokenExpirationTime: Long,
    private val validator: Validator
) : UsernamePasswordAuthenticationFilter(authenticationManager) {

    override fun attemptAuthentication(req: HttpServletRequest, res: HttpServletResponse): Authentication {
        val creds = ObjectMapper().readValue(req.inputStream, LoginCredentials::class.java)

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

        val secretKeyBytes = Base64.getEncoder().encode(tokenSecret.toByteArray())
        val secretKey = Keys.hmacShaKeyFor(secretKeyBytes)

        val now = Instant.now()

        val token = Jwts.builder()
            .subject(userDetails.userId!!)
            .expiration(Date.from(now.plusMillis(tokenExpirationTime)))
            .issuedAt(Date.from(now))
            .signWith(secretKey)
            .compact()

        res.addHeader("token", token)
        res.addHeader("userId", userDetails.userId)
    }
}
