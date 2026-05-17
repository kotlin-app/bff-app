package com.example.bff.service

import com.example.bff.model.Review
import com.github.benmanes.caffeine.cache.Cache
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration

// レビューサービスへの呼び出しとキャッシュ管理を担うサービスクラス
// レビューは商品に対する補助情報のため、サービス障害時は空リストを返して処理を継続する（フォールバック）
@Service
class ReviewService(
    @Qualifier("reviewWebClient") private val webClient: WebClient,
    private val reviewCache: Cache<Long, List<Review>>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 指定商品IDのレビュー一覧を取得する
    // キャッシュに存在する場合はバックエンドを呼ばずに即返す
    // キャッシュにない場合はレビューサービスを呼び出し、結果をキャッシュに保存する
    fun getReviews(productId: Long, simulate: String = "none"): Mono<List<Review>> {
        // キャッシュヒット：バックエンドを呼ばずに即返す
        reviewCache.getIfPresent(productId)?.let { cached ->
            log.info("Cache HIT  : productId=$productId (${cached.size}件)")
            return Mono.just(cached)
        }

        // キャッシュミス：バックエンドを呼んでキャッシュに保存する
        log.info("Cache MISS : productId=$productId → review-serviceを呼び出し")
        return webClient.get()
            .uri("/product/$productId?simulate=$simulate")
            .retrieve()
            .bodyToFlux(Review::class.java)
            .collectList()
            .timeout(Duration.ofSeconds(3))
            .doOnSuccess { reviews ->
                reviewCache.put(productId, reviews)
                log.info("Cache SET  : productId=$productId (${reviews.size}件をキャッシュ)")
            }
            // レビューは補助情報のため、障害時は空リストを返して処理を継続する
            .onErrorResume { ex ->
                log.warn("Review service failed for productId=$productId: ${ex.message}")
                Mono.just(emptyList())
            }
    }

    // キャッシュの統計情報を返す（ヒット率・ミス数・保持件数など）
    fun getCacheStats(): Map<String, Any> {
        val stats = reviewCache.stats()
        return mapOf(
            "hitCount"    to stats.hitCount(),
            "missCount"   to stats.missCount(),
            "hitRate"     to "%.1f%%".format(stats.hitRate() * 100),
            "cachedItems" to reviewCache.estimatedSize(),
        )
    }

    // キャッシュを全件削除する
    fun clearCache() {
        reviewCache.invalidateAll()
        log.info("Cache cleared")
    }
}
