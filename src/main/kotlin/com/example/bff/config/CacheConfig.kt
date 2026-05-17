package com.example.bff.config

import com.example.bff.model.Review
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

// レビューデータをBFF層でキャッシュするための設定クラス
// キャッシュすることでレビューサービスへの呼び出し回数を削減し、バックエンドの負荷を軽減する
// 最大件数・有効期限は application.yml で管理する
@Configuration
class CacheConfig(
    @Value("\${cache.reviews.max-size}") private val maxSize: Long,
    @Value("\${cache.reviews.expire-minutes}") private val expireMinutes: Long,
) {

    // productId をキーにレビューリストをキャッシュする Caffeine キャッシュ Bean
    @Bean
    fun reviewCache(): Cache<Long, List<Review>> =
        Caffeine.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(expireMinutes, TimeUnit.MINUTES)
            .recordStats() // ヒット率などの統計を記録（/api/cache/stats で確認できる）
            .build()
}
