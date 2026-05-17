package com.example.bff.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig(
    @Value("\${services.product-url}") private val productUrl: String,
    @Value("\${services.review-url}") private val reviewUrl: String,
    @Value("\${services.order-url}") private val orderUrl: String,
) {

    @Bean
    fun productWebClient(): WebClient =
        WebClient.builder().baseUrl("$productUrl/products").build()

    @Bean
    fun reviewWebClient(): WebClient =
        WebClient.builder().baseUrl("$reviewUrl/reviews").build()

    @Bean
    fun orderWebClient(): WebClient =
        WebClient.builder().baseUrl("$orderUrl/api/orders").build()
}
