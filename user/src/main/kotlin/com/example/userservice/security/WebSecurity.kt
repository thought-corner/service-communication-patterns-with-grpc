package com.example.userservice.security

import com.example.userservice.service.UserService
import jakarta.validation.Validator
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.IpAddressMatcher
import org.springframework.http.HttpMethod
import java.util.function.Supplier

@Configuration
@EnableWebSecurity
class WebSecurity(
    @Value("\${token.secret}") private val tokenSecret: String,
    @Value("\${token.expiration_time}") private val tokenExpirationTime: Long,
    private val userService: UserService,
    private val userDetailsService: UserDetailsServiceImpl,
    private val bCryptPasswordEncoder: BCryptPasswordEncoder,
    private val validator: Validator
) {

    companion object {
        const val ALLOWED_IP_ADDRESS = "127.0.0.1"
        const val SUBNET = "/32"
        val ALLOWED_IP_ADDRESS_MATCHER = IpAddressMatcher(ALLOWED_IP_ADDRESS + SUBNET)
    }

    @Bean
    protected fun configure(http: HttpSecurity): SecurityFilterChain {
        val authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder::class.java)
        authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(bCryptPasswordEncoder)

        val authenticationManager: AuthenticationManager = authenticationManagerBuilder.build()

        http.csrf { csrf -> csrf.disable() }

        http.authorizeHttpRequests { authz ->
            authz
                .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/actuator/**")).permitAll()
                .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/h2-console/**")).permitAll()
                .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/users"))
                .permitAll()
                .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/welcome")).permitAll()
                .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/health-check")).permitAll()
                .requestMatchers("/**").access(
                    WebExpressionAuthorizationManager("hasIpAddress('127.0.0.1') or hasIpAddress('::1')")
                )
                .anyRequest().authenticated()
        }
            .authenticationManager(authenticationManager)
            .sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

        http.addFilter(getAuthenticationFilter(authenticationManager))

        http.addFilterBefore(IpAddressLoggingFilter(), UsernamePasswordAuthenticationFilter::class.java)

        http.headers { headers -> headers.frameOptions { frameOptions -> frameOptions.sameOrigin() } }

        return http.build()
    }

    private fun hasIpAddress(
        authentication: Supplier<Authentication>,
        context: RequestAuthorizationContext
    ): AuthorizationDecision {
        return AuthorizationDecision(ALLOWED_IP_ADDRESS_MATCHER.matches(context.request))
    }

    private fun getAuthenticationFilter(authenticationManager: AuthenticationManager): AuthenticationFilter {
        return AuthenticationFilter(authenticationManager, userService, tokenSecret, tokenExpirationTime, validator)
    }
}
