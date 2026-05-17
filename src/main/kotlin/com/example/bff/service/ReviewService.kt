package com.example.bff.service

import com.example.bff.model.Review
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class ReviewService(
    @Qualifier("reviewWebClient") private val webClient: WebClient,
    private val redis: ReactiveStringRedisTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${cache.reviews.expire-minutes}") private val expireMinutes: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun cacheKey(productId: Long) = "reviews:$productId"

    fun getReviews(productId: Long, simulate: String = "none"): Mono<List<Review>> {
        val key = cacheKey(productId)
        return redis.opsForValue().get(key)
            .map { json ->
                val reviews: List<Review> = objectMapper.readValue(json)
                log.info("Cache HIT: productId=$productId (${reviews.size}件)")
                reviews
            }
            .switchIfEmpty(
                webClient.get()
                    .uri("/product/$productId?simulate=$simulate")
                    .retrieve()
                    .bodyToFlux(Review::class.java)
                    .collectList()
                    .timeout(Duration.ofSeconds(3))
                    .flatMap { reviews ->
                        val json = objectMapper.writeValueAsString(reviews)
                        redis.opsForValue()
                            .set(key, json, Duration.ofMinutes(expireMinutes))
                            .doOnSuccess { log.info("Cache SET: productId=$productId (${reviews.size}件)") }
                            .thenReturn(reviews)
                    }
                    .onErrorResume { ex ->
                        log.warn("Review service failed for productId=$productId: ${ex.message}")
                        Mono.just(emptyList())
                    }
            )
    }

    fun getCacheStats(): Mono<Map<String, Any>> =
        redis.keys("reviews:*")
            .count()
            .map { count -> mapOf("store" to "redis", "cachedItems" to count) }

    fun clearCache(): Mono<Long> =
        redis.keys("reviews:*")
            .collectList()
            .flatMap { keys ->
                if (keys.isEmpty()) Mono.just(0L)
                else redis.delete(*keys.toTypedArray())
            }
            .doOnSuccess { count -> log.info("Cache cleared: $count keys deleted") }
}
