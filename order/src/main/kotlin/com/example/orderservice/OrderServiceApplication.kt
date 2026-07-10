package com.example.orderservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class OrderServiceApplication

fun main(args: Array<String>) {
	runApplication<OrderServiceApplication>(*args)
}
