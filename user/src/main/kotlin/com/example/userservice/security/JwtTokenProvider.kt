package com.example.userservice.security

import io.github.oshai.kotlinlogging.KotlinLogging
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Base64
import java.util.Date

private val log = KotlinLogging.logger {}

@Component
class JwtTokenProvider(
    @Value("\${token.secret}") secret: String,
    @Value("\${token.expiration_time}") private val expirationTime: Long,
) {
    private val secretKey = Keys.hmacShaKeyFor(Base64.getEncoder().encode(secret.toByteArray()))

    fun generate(userId: String): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(userId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(expirationTime)))
            .signWith(secretKey)
            .compact()
    }

    fun getUserId(token: String): String? {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
                .subject
        } catch (ex: ExpiredJwtException) {
            log.debug { "Rejected expired JWT: ${ex.message}" }
            null
        } catch (ex: JwtException) {
            log.debug { "Rejected invalid JWT: ${ex.message}" }
            null
        } catch (ex: IllegalArgumentException) {
            log.debug { "Rejected malformed JWT: ${ex.message}" }
            null
        }
    }
}
