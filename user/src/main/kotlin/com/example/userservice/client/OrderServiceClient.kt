package com.example.userservice.client

import com.example.order.v1.GetOrdersRequest
import com.example.order.v1.OrderQueryServiceGrpc.OrderQueryServiceBlockingStub
import com.example.userservice.config.decorateWithResilience
import com.example.userservice.controller.dto.order.OrderResponse
import com.example.userservice.exception.OrdersUnavailableException
import com.example.userservice.service.dto.OrdersResult
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import io.grpc.StatusRuntimeException
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

private const val DEADLINE_SECONDS = 4L

/**
 * order-service 주문 조회의 gRPC 클라이언트.
 *
 * 재시도/서킷은 [decorateWithResilience]가 공유(CircuitBreaker(외)→Retry(내)) 처리한다.
 * 종착 실패(재시도 소진/서킷 OPEN) 시 엔드포인트 성격에 따라 분기가 다르다:
 * - [getOrdersOrDegrade] (부가): 조용히 [OrdersResult.unavailable]로 degrade
 * - [getOrdersOrThrow]  (필수): [OrdersUnavailableException]으로 하드 실패
 */
@Component
class OrderServiceClient(
    private val orderQueryStub: OrderQueryServiceBlockingStub,
    private val orderRetry: Retry,
    private val orderCircuitBreaker: CircuitBreaker
) {

    /** 부가 데이터용: 종착 실패 시 UNAVAILABLE로 정직하게 degrade한다. */
    fun getOrdersOrDegrade(userId: String): OrdersResult =
        try {
            OrdersResult.ok(fetchOrders(userId))
        } catch (e: CallNotPermittedException) {
            log.warn { "Circuit OPEN for order-service, degrading orders to UNAVAILABLE for user=$userId" }
            OrdersResult.unavailable()
        } catch (e: StatusRuntimeException) {
            log.error(e) { "order-service gRPC call failed for user=$userId, degrading to UNAVAILABLE" }
            OrdersResult.unavailable()
        } catch (e: Exception) {
            // 부가 조회는 어떤 이유로도 500을 내지 않는다. 프로토 매핑/파싱(LocalDateTime.parse) 등
            // gRPC status가 아닌 실패까지 여기서 흡수해 degrade로 종착시킨다.
            log.error(e) { "Unexpected error resolving orders for user=$userId, degrading to UNAVAILABLE" }
            OrdersResult.unavailable()
        }

    /** 필수 데이터용: 종착 실패 시 하드 실패(예외)로 올려 503으로 매핑되게 한다. */
    fun getOrdersOrThrow(userId: String): OrdersResult =
        try {
            OrdersResult.ok(fetchOrders(userId))
        } catch (e: CallNotPermittedException) {
            log.warn { "Circuit OPEN for order-service, failing hard for essential orders user=$userId" }
            throw OrdersUnavailableException(cause = e)
        } catch (e: StatusRuntimeException) {
            log.error(e) { "order-service gRPC call failed for user=$userId, failing hard" }
            throw OrdersUnavailableException(cause = e)
        } catch (e: Exception) {
            // 부가 경로(getOrdersOrDegrade)와 대칭: 프로토 매핑/파싱(LocalDateTime.parse) 등
            // gRPC status가 아닌 실패도 "주문을 못 준 것"으로 보고 503(하드 실패)으로 종착시켜
            // 500 누수를 막는다. 동일 fetchOrders를 쓰는 두 경로가 같은 downstream 이상에 대해
            // 일관된 신호(부가=UNAVAILABLE, 필수=503+Retry-After)를 내도록 한다.
            log.error(e) { "Unexpected error resolving essential orders for user=$userId, failing hard" }
            throw OrdersUnavailableException(cause = e)
        }

    private fun fetchOrders(userId: String): List<OrderResponse> =
        decorateWithResilience(orderRetry, orderCircuitBreaker) {
            val request = GetOrdersRequest.newBuilder().setUserId(userId).build()
            val response = orderQueryStub
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .getOrders(request)
            response.ordersList.map { OrderProtoMapper.toResponse(it) }
        }
}
