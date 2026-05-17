package com.example.bff.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

// BFFが各マイクロサービスを呼び出すための WebClient を定義する設定クラス
// サービスごとに baseUrl を設定し、@Qualifier で注入先を識別する
@Configuration
class WebClientConfig {

    // 商品サービスへのHTTPクライアント
    @Bean
    fun productWebClient(): WebClient =
        WebClient.builder()
            .baseUrl("http://localhost:8080/mock/products")
            .build()

    // レビューサービスへのHTTPクライアント
    @Bean
    fun reviewWebClient(): WebClient =
        WebClient.builder()
            .baseUrl("http://localhost:8080/mock/reviews")
            .build()
}
