package com.example.userservice.config

import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class RestTemplateConfig {

	@Bean
	fun getRestTemplate(): RestTemplate {
		val timeout = 5000L

		return RestTemplateBuilder()
			.connectTimeout(Duration.ofMillis(timeout))
			.readTimeout(Duration.ofMillis(timeout))
			.build()
	}
}
