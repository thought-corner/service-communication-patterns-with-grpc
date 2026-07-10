package com.example.userservice.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null,

	@Column(nullable = false, length = 50, unique = true)
	var email: String? = null,

	@Column(nullable = false, length = 50)
	var name: String? = null,

	@Column(nullable = false, unique = true)
	var userId: String? = null,

	@Column(nullable = false, unique = true)
	var password: String? = null
)
