package com.example.bff.controller

import com.example.bff.service.ReviewService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// キャッシュの管理エンドポイントを提供するコントローラー
// ADMINロールを持つユーザーのみアクセス可能（SecurityConfig で制限）
@RestController
@RequestMapping("/api/cache")
class CacheController(private val reviewService: ReviewService) {

    // GET /api/cache/stats
    // キャッシュのヒット数・ミス数・ヒット率・保持件数を返す
    @GetMapping("/stats")
    fun getStats() = reviewService.getCacheStats()

    // DELETE /api/cache
    // キャッシュを全件削除する（動作確認やキャッシュ不整合の解消に使用）
    @DeleteMapping
    fun clearCache(): Map<String, String> {
        reviewService.clearCache()
        return mapOf("message" to "Cache cleared")
    }
}
