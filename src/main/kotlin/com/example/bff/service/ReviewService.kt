package com.example.bff.service

import com.example.bff.model.Review
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory
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
    private val cbFactory: ReactiveCircuitBreakerFactory<*, *>,
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
            .switchIfEmpty(fetchAndCache(productId, key, simulate))
    }

    private fun fetchAndCache(productId: Long, key: String, simulate: String): Mono<List<Review>> {
        val call = webClient.get()
            .uri("/product/$productId?simulate=$simulate")
            .retrieve()
            .bodyToFlux(Review::class.java)
            .collectList()
            .flatMap { reviews ->
                val json = objectMapper.writeValueAsString(reviews)
                redis.opsForValue()
                    .set(key, json, Duration.ofMinutes(expireMinutes))
                    .doOnSuccess { log.info("Cache SET: productId=$productId (${reviews.size}件)") }
                    .thenReturn(reviews)
            }

        return cbFactory.create("review-service").run(call) { ex ->
            log.warn("review-service circuit open for productId=$productId: ${ex.message}")
            Mono.just(emptyList())
        }
    }

    fun postReview(body: Map<String, Any>, userName: String): Mono<Any> {
        val productId = (body["productId"] as? Number)?.toLong() ?: return Mono.error(IllegalArgumentException("productId required"))
        val call = webClient.post()
            .header("X-User-Name", userName)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Any::class.java)
            .flatMap { result ->
                redis.delete(cacheKey(productId))
                    .doOnSuccess { log.info("Cache invalidated for productId=$productId after review post") }
                    .thenReturn(result)
            }

        return cbFactory.create("review-service").run(call) { ex ->
            log.error("review-service circuit open (postReview): {}", ex.message)
            Mono.error(RuntimeException("レビューサービスが利用できません"))
        }
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
