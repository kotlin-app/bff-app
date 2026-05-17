package com.example.bff.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

// BFFが各マイクロサービスを呼び出すための WebClient を定義する設定クラス
// ベースURLは環境変数で切り替えられるため、ローカル・Docker・本番環境で同じコードが使える
@Configuration
class WebClientConfig(
    // ローカル: http://localhost:8081、Docker: http://product-service:8081
    @Value("\${services.product-url}") private val productUrl: String,
    // ローカル: http://localhost:8082、Docker: http://review-service:8082
    @Value("\${services.review-url}") private val reviewUrl: String,
) {

    @Bean
    fun productWebClient(): WebClient =
        WebClient.builder()
            .baseUrl("$productUrl/products")
            .build()

    @Bean
    fun reviewWebClient(): WebClient =
        WebClient.builder()
            .baseUrl("$reviewUrl/reviews")
            .build()
}
