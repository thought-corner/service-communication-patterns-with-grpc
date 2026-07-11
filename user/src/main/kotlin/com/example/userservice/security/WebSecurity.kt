package com.example.userservice.security

import com.example.userservice.service.UserService
import jakarta.validation.Validator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.http.HttpMethod

@Configuration
@EnableWebSecurity
class WebSecurity(
    private val userService: UserService,
    private val userDetailsService: UserDetailsServiceImpl,
    private val bCryptPasswordEncoder: BCryptPasswordEncoder,
    private val validator: Validator,
    private val tokenProvider: JwtTokenProvider
) {

    @Bean
    protected fun configure(http: HttpSecurity): SecurityFilterChain {
        val authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder::class.java)
        authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(bCryptPasswordEncoder)

        val authenticationManager: AuthenticationManager = authenticationManagerBuilder.build()

        http.csrf { csrf -> csrf.disable() }

        http.authorizeHttpRequests { authz ->
            authz
                .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/actuator/**")).permitAll()
                .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/users"))
                .permitAll()
                .anyRequest().authenticated()
        }
            .authenticationManager(authenticationManager)
            .sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

        http.addFilter(getAuthenticationFilter(authenticationManager))
        http.addFilterBefore(JwtAuthorizationFilter(tokenProvider), UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    private fun getAuthenticationFilter(authenticationManager: AuthenticationManager): AuthenticationFilter {
        return AuthenticationFilter(authenticationManager, userService, tokenProvider, validator)
    }
}
