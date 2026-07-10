package com.example.userservice.config

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder
import org.springframework.cloud.client.circuitbreaker.Customizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class Resilience4JConfig {

    @Bean
    fun globalCustomConfiguration(): Customizer<Resilience4JCircuitBreakerFactory> {
        val circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(4f)
            .waitDurationInOpenState(Duration.ofMillis(1000))
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(2)
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
    fun specificCustomConfiguration1(): Customizer<Resilience4JCircuitBreakerFactory> {
        val circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(6f)
            .waitDurationInOpenState(Duration.ofMillis(1000))
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(3)
            .build()

        val timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(4))
            .build()

        return Customizer { factory ->
            factory.configure(
                { builder ->
                    builder.circuitBreakerConfig(circuitBreakerConfig).timeLimiterConfig(timeLimiterConfig).build()
                },
                "circuitBreaker1"
            )
        }
    }

    @Bean
    fun specificCustomConfiguration2(): Customizer<Resilience4JCircuitBreakerFactory> {
        val circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(8f)
            .waitDurationInOpenState(Duration.ofMillis(1000))
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(4)
            .build()

        val timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(4))
            .build()

        return Customizer { factory ->
            factory.configure(
                { builder ->
                    builder.circuitBreakerConfig(circuitBreakerConfig).timeLimiterConfig(timeLimiterConfig).build()
                },
                "circuitBreaker2"
            )
        }
    }
}
