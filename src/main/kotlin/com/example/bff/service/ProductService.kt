package com.example.bff.service

import com.example.bff.exception.ProductNotFoundException
import com.example.bff.exception.ServiceUnavailableException
import com.example.bff.model.Product
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class ProductService(@Qualifier("productWebClient") private val webClient: WebClient) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun getProduct(id: Long): Mono<Product> =
        webClient.get()
            .uri("/$id")
            .retrieve()
            .onStatus({ it == HttpStatus.NOT_FOUND }) {
                log.warn("product not found: id={}", id)
                Mono.error(ProductNotFoundException(id))
            }
            .bodyToMono(Product::class.java)
            .timeout(Duration.ofSeconds(3))
            .doOnSuccess { log.info("product fetched: id={}", id) }
            .onErrorResume { ex ->
                when (ex) {
                    is ProductNotFoundException -> Mono.error(ex)
                    else -> {
                        log.error("product-service unavailable: id={} error={}", id, ex.message)
                        Mono.error(ServiceUnavailableException("product-service"))
                    }
                }
            }
}
