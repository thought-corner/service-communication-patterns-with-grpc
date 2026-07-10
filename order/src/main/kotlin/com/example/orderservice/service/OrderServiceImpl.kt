package com.example.orderservice.service

import com.example.orderservice.controller.dto.OrderRequest
import com.example.orderservice.entity.Order
import com.example.orderservice.repository.OrderRepository
import com.example.orderservice.service.dto.OrderResult
import com.example.orderservice.vo.OrderItem
import org.modelmapper.ModelMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class OrderServiceImpl(
    private val orderRepository: OrderRepository,
    private val modelMapper: ModelMapper
) : OrderService {

    @Transactional
    override fun createOrder(orderRequest: OrderRequest, userId: String): OrderResult {
        val orderItem = OrderItem(
            productId = orderRequest.productId,
            qty = orderRequest.qty,
            unitPrice = orderRequest.unitPrice,
            userId = userId
        )
        orderItem.orderId = UUID.randomUUID().toString()
        orderItem.totalPrice = orderItem.calculateTotalPrice()

        val order = modelMapper.map(orderItem, Order::class.java)
        val savedOrder = orderRepository.save(order)
        return OrderResult.from(modelMapper.map(savedOrder, OrderItem::class.java))
    }

    override fun getOrderByOrderId(orderId: String): OrderResult? {
        val order = orderRepository.findByOrderId(orderId) ?: return null
        return OrderResult.from(modelMapper.map(order, OrderItem::class.java))
    }

    override fun getOrdersByUserId(userId: String): List<OrderResult> {
        return orderRepository.findByUserId(userId).map { OrderResult.from(modelMapper.map(it, OrderItem::class.java)) }
    }
}
