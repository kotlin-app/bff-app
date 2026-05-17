package com.example.bff.service

import com.example.bff.exception.ProductNotFoundException
import com.example.bff.exception.ServiceUnavailableException
import com.example.bff.model.Product
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class ProductService(
    @Qualifier("productWebClient") private val webClient: WebClient,
    private val cbFactory: ReactiveCircuitBreakerFactory<*, *>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getProducts(): Mono<List<Product>> {
        val call = webClient.get()
            .retrieve()
            .bodyToFlux(Product::class.java)
            .collectList()
            .doOnSuccess { log.info("products fetched: count={}", it.size) }

        return cbFactory.create("product-service").run(call) { ex ->
            log.error("product-service circuit open: {}", ex.message)
            Mono.error(ServiceUnavailableException("product-service"))
        }
    }

    fun getProduct(id: Long): Mono<Product> {
        val call = webClient.get()
            .uri("/$id")
            .retrieve()
            .onStatus({ it == HttpStatus.NOT_FOUND }) {
                log.warn("product not found: id={}", id)
                Mono.error(ProductNotFoundException(id))
            }
            .bodyToMono(Product::class.java)
            .doOnSuccess { log.info("product fetched: id={}", id) }

        return cbFactory.create("product-service").run(call) { ex ->
            when (ex) {
                is ProductNotFoundException -> Mono.error(ex)
                else -> {
                    log.error("product-service circuit open: id={} error={}", id, ex.message)
                    Mono.error(ServiceUnavailableException("product-service"))
                }
            }
        }
    }
}
