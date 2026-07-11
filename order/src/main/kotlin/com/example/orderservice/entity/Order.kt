package com.example.orderservice.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.Date

@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener::class)
class Order(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null,

	@Column(nullable = false, length = 120)
	var productId: String? = null,

	@Column(nullable = false)
	var qty: Int? = null,

	@Column(nullable = false)
	var unitPrice: Int? = null,

	@Column(nullable = false)
	var totalPrice: Int? = null,

	@Column(nullable = false)
	var userId: String? = null,

	@Column(nullable = false, unique = true)
	var orderId: String? = null,

	@CreatedDate
	@Column(nullable = false, updatable = false)
	var createdAt: Date? = null,

	@LastModifiedDate
	@Column(nullable = false)
	var updatedAt: Date? = null
) {

	@PrePersist
	@PreUpdate
	protected fun calculateTotalPrice() {
		totalPrice = (qty ?: 0) * (unitPrice ?: 0)
	}

	companion object {
		fun place(productId: String, qty: Int, unitPrice: Int, userId: String, orderId: String): Order {
			require(qty > 0) { "qty must be greater than zero" }
			require(unitPrice > 0) { "unitPrice must be greater than zero" }
			return Order(
				productId = productId,
				qty = qty,
				unitPrice = unitPrice,
				totalPrice = qty * unitPrice,
				userId = userId,
				orderId = orderId
			)
		}
	}
}
