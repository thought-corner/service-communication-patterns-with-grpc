package com.example.userservice.config

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * order-service를 향한 gRPC 호출을 감싸는 공유 재시도/서킷 브레이커 계층.
 *
 * 데코레이터 순서는 CircuitBreaker(외) → Retry(내)다. 이 순서가 주는 두 가지:
 * - 서킷이 OPEN이면 바깥 CircuitBreaker가 CallNotPermittedException을 즉시 던져 재시도조차 시작하지 않는다(fail-fast). 다운스트림이 죽었을 때 재시도로 부하를 증폭시키지 않는다.
 * - 재시도가 안쪽에 있으므로 1 논리 호출이 CB window에 결과 1건만 적재된다. 재시도가 일시적 blip을 흡수해 성공하면 CB는 성공 1건으로 본다(attempt마다 실패를 중복 적재하지 않음).
 *
 * 재시도/서킷 집계 대상은 "일시적" gRPC status(UNAVAILABLE, RESOURCE_EXHAUSTED)뿐이다.
 * NOT_FOUND/INVALID_ARGUMENT 같은 호출자 책임 오류는 재시도하지도, 서킷 실패로 세지도 않는다.
 */
@Configuration
class Resilience4JConfig {

    @Bean
    fun orderCircuitBreakerRegistry(): CircuitBreakerRegistry {
        val config = CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(50f)
            .slowCallRateThreshold(50f)
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .permittedNumberOfCallsInHalfOpenState(3)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            // 일시적 gRPC status만 실패로 집계 → 4xx성 오류(NOT_FOUND 등)로는 서킷이 열리지 않음
            .recordException(::isRetryableGrpcException)
            .build()
        return CircuitBreakerRegistry.of(config)
    }

    @Bean
    fun orderCircuitBreaker(registry: CircuitBreakerRegistry): CircuitBreaker =
        registry.circuitBreaker("orderService")

    @Bean
    fun orderRetryRegistry(): RetryRegistry {
        val config = RetryConfig.custom<Any>()
            .maxAttempts(3)
            // 지수 백오프 + jitter로 동시 재시도 몰림(thundering herd)을 분산
            .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(Duration.ofMillis(200), 2.0, 0.5))
            .retryOnException(::isRetryableGrpcException)
            .build()
        return RetryRegistry.of(config)
    }

    @Bean
    fun orderRetry(registry: RetryRegistry): Retry =
        registry.retry("orderService")
}

/** 재시도/서킷 집계 대상이 되는 "일시적" gRPC status 집합. */
val RETRYABLE_GRPC_CODES: Set<Status.Code> = setOf(
    Status.Code.UNAVAILABLE,
    Status.Code.RESOURCE_EXHAUSTED
)

fun isRetryableGrpcException(throwable: Throwable): Boolean =
    throwable is StatusRuntimeException && throwable.status.code in RETRYABLE_GRPC_CODES

/**
 * CircuitBreaker(외) → Retry(내) 순으로 supplier를 감싸 실행한다.
 * Retry가 먼저 supplier를 감싸고, 그 위를 CircuitBreaker가 감싼다.
 * 따라서 CB는 재시도가 끝난 "논리 호출" 단위로 성공/실패를 1건만 집계하고,
 * 서킷이 OPEN이면 재시도가 시작되기 전에 fail-fast한다.
 */
fun <T> decorateWithResilience(retry: Retry, circuitBreaker: CircuitBreaker, supplier: () -> T): T {
    val retried = Retry.decorateSupplier(retry) { supplier() }
    return CircuitBreaker.decorateSupplier(circuitBreaker, retried).get()
}
