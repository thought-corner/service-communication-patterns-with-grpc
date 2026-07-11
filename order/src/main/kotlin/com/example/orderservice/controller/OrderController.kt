package com.example.orderservice.controller

import com.example.orderservice.service.OrderService
import com.example.orderservice.controller.dto.OrderRequest
import com.example.orderservice.controller.dto.OrderResponse
import com.example.orderservice.controller.dto.OrderResponseList
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderService: OrderService
) {

    @PostMapping("/{userId}")
    fun createOrder(
        @PathVariable userId: String,
        @Valid @RequestBody orderRequest: OrderRequest
    ): ResponseEntity<OrderResponse> {
        val result = orderService.createOrder(orderRequest, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(result))
    }

    @GetMapping("/{userId}")
    fun getOrder(@PathVariable userId: String): ResponseEntity<OrderResponseList> {
        val results = orderService.getOrdersByUserId(userId)
        return ResponseEntity.status(HttpStatus.OK).body(OrderResponseList.from(results))
    }
}
