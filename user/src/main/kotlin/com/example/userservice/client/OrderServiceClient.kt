package com.example.userservice.client

import com.example.userservice.controller.dto.order.OrderResponseList
import com.example.userservice.service.dto.OrdersResult
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.util.concurrent.TimeoutException

private val log = KotlinLogging.logger {}

@Component
class OrderServiceClient(
    private val restTemplate: RestTemplate,
    private val circuitBreakerFactory: CircuitBreakerFactory<*, *>,
    @Value("\${order-service.url}") private val orderServiceUrl: String
) {

    fun getOrders(userId: String): OrdersResult {
        val circuitBreaker = circuitBreakerFactory.create("orderService")
        return circuitBreaker.run(
            { fetchOrders(userId) },
            { throwable -> handleFailure(userId, throwable) }
        )
    }

    private fun fetchOrders(userId: String): OrdersResult {
        val orderUrl = "$orderServiceUrl/orders/$userId"
        val response = restTemplate.exchange(orderUrl, HttpMethod.GET, null, OrderResponseList::class.java)
        return OrdersResult.ok(response.body?.orders ?: emptyList())
    }

    private fun handleFailure(userId: String, throwable: Throwable?): OrdersResult {
        when (throwable) {
            is CallNotPermittedException ->
                log.warn { "Circuit OPEN for order-service, marking orders UNAVAILABLE for user=$userId" }

            is TimeoutException, is RestClientException ->
                log.error(throwable) { "order-service call failed for user=$userId, marking orders UNAVAILABLE" }

            else -> {
                log.error(throwable) { "Unexpected error while fetching orders for user=$userId" }
                throw throwable ?: IllegalStateException("Order fetch failed without a cause for user=$userId")
            }
        }
        return OrdersResult.unavailable()
    }
}
