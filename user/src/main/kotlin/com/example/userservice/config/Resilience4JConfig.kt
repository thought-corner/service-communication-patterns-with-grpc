package com.example.userservice.config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import org.springframework.boot.ApplicationRunner
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder
import org.springframework.cloud.client.circuitbreaker.Customizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import java.time.Duration
import java.util.concurrent.TimeoutException

private val log = KotlinLogging.logger {}

/**
 * order-service 동기 REST 호출을 감싸는 Resilience4j 서킷 브레이커 설정.
 *
 * order-service 장애가 user-service로 전파되는 것(cascading failure)을 막기 위해 모든 서킷 브레이커의 기본 정책을 정의한다.
 *
 * 핵심 설계 결정:
 * - slidingWindowSize(10) >= minimumNumberOfCalls(5): 실패율은 최소 호출 수가 윈도우에 쌓여야 계산되므로, minimumNumberOfCalls가 윈도우보다 크면 서킷이 영원히 열리지 않는다.
 * 이 불변식을 반드시 지킨다.
 * - failureRateThreshold(50%) / slowCallRateThreshold(50%): 절반 이상 실패하거나 2초를 넘는 느린 호출이 절반 이상이면 OPEN으로 전환한다.
 * - recordExceptions / ignoreExceptions: 다운스트림 장애(RestClientException, TimeoutException)만 실패로 집계하고, 4xx(HttpClientErrorException)는 호출자 잘못이므로 서킷을 여는 데 카운트하지 않는다.
 * - TimeLimiter(4초): 서킷 브레이커만으로는 못 막는 느린 응답을 타임아웃으로 차단한다.
 *
 * 상태 전이(CLOSED/OPEN/HALF_OPEN)는 CircuitBreakerRegistry 이벤트 리스너로 로깅하여 장애·회복 과정을 관측할 수 있게 한다.
 */
@Configuration
class Resilience4JConfig {

    @Bean
    fun defaultCircuitBreakerCustomizer(): Customizer<Resilience4JCircuitBreakerFactory> {
        val circuitBreakerConfig = CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(50f)
            .slowCallRateThreshold(50f)
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .permittedNumberOfCallsInHalfOpenState(3)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(RestClientException::class.java, TimeoutException::class.java)
            .ignoreExceptions(HttpClientErrorException::class.java)
            .build()

        val timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(4))
            .build()

        return Customizer { factory ->
            factory.configureDefault { id ->
                Resilience4JConfigBuilder(id)
                    .timeLimiterConfig(timeLimiterConfig)
                    .circuitBreakerConfig(circuitBreakerConfig)
                    .build()
            }
        }
    }

    @Bean
    fun circuitBreakerEventLogger(registry: CircuitBreakerRegistry): ApplicationRunner {
        return ApplicationRunner {
            registry.allCircuitBreakers.forEach(::registerStateLogging)
            registry.eventPublisher.onEntryAdded { registerStateLogging(it.addedEntry) }
        }
    }

    private fun registerStateLogging(circuitBreaker: CircuitBreaker) {
        circuitBreaker.eventPublisher.onStateTransition {
            log.warn { "[CircuitBreaker '${it.circuitBreakerName}'] ${it.stateTransition}" }
        }
    }
}
