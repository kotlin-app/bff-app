package com.example.bff.service

import com.example.bff.exception.ProductNotFoundException
import com.example.bff.exception.ServiceUnavailableException
import com.example.bff.model.Product
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration

// 商品サービスへの呼び出しを担うサービスクラス
// WebClient でノンブロッキングにHTTPリクエストを行い、エラーをBFF固有の例外に変換する
@Service
class ProductService(@Qualifier("productWebClient") private val webClient: WebClient) {

    // 指定IDの商品を商品サービスから取得する
    // 404 → ProductNotFoundException に変換（クライアントに正確なエラーを伝える）
    // タイムアウト・その他エラー → ServiceUnavailableException に変換
    fun getProduct(id: Long): Mono<Product> =
        webClient.get()
            .uri("/$id")
            .retrieve()
            .onStatus({ it == HttpStatus.NOT_FOUND }) {
                Mono.error(ProductNotFoundException(id))
            }
            .bodyToMono(Product::class.java)
            .timeout(Duration.ofSeconds(3))
            .onErrorResume { ex ->
                when (ex) {
                    // ProductNotFoundException はそのまま再スロー（GlobalExceptionHandler で404を返す）
                    is ProductNotFoundException -> Mono.error(ex)
                    // それ以外はすべて上流サービス障害として扱う
                    else -> Mono.error(ServiceUnavailableException("product-service"))
                }
            }
}
