package com.example.userservice.security

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

private val log = KotlinLogging.logger {}

class IpAddressLoggingFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val ip = extractIpAddress(request)
        val requestURI = request.requestURI

        log.info { "Access attempt to $requestURI from IP: $ip" }

        filterChain.doFilter(request, response)
    }

    private fun extractIpAddress(request: HttpServletRequest): String? {
        var ip = request.getHeader("X-Forwarded-For")
        if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("Proxy-Client-IP")
        }
        if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("WL-Proxy-Client-IP")
        }
        if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("HTTP_CLIENT_IP")
        }
        if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR")
        }
        if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.remoteAddr
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim()
        }

        return ip
    }
}
