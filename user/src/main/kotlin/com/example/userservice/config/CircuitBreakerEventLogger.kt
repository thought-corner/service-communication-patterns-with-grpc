package com.example.userservice.config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * 서킷 브레이커 상태 전이(CLOSED→OPEN 등)를 로깅한다.
 * 기존 브레이커뿐 아니라 이후 레지스트리에 추가되는 브레이커에도 자동으로 리스너를 건다.
 */
@Component
class CircuitBreakerEventLogger(
    private val registry: CircuitBreakerRegistry
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        registry.allCircuitBreakers.forEach(::registerStateLogging)
        registry.eventPublisher.onEntryAdded { registerStateLogging(it.addedEntry) }
    }

    private fun registerStateLogging(circuitBreaker: CircuitBreaker) {
        circuitBreaker.eventPublisher.onStateTransition {
            log.warn { "[CircuitBreaker '${it.circuitBreakerName}'] ${it.stateTransition}" }
        }
    }
}
