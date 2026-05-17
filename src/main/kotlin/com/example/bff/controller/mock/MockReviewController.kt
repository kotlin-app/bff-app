package com.example.bff.controller.mock

import com.example.bff.model.Review
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

// レビューマイクロサービスを模倣するモックコントローラー
// simulate パラメータで障害やレイテンシを再現し、BFF のエラーハンドリングを検証できる
@RestController
@RequestMapping("/mock/reviews")
class MockReviewController {

    // インメモリのレビューデータ（実際は DB や外部サービスから取得する）
    private val reviews = mapOf(
        1L to listOf(
            Review(1, 1, "田中さん", 5, "朝のコーヒーが格段においしくなりました！"),
            Review(2, 1, "佐藤さん", 4, "操作が簡単で気に入っています。"),
            Review(3, 1, "鈴木さん", 3, "価格の割には普通かな。"),
        ),
        2L to listOf(
            Review(4, 2, "山田さん", 5, "音質が最高です。通勤に欠かせません。"),
            Review(5, 2, "中村さん", 5, "ノイズキャンセリングが強力で集中できます。"),
        ),
        3L to listOf(
            Review(6, 3, "小林さん", 4, "軽くて持ち運びやすい。保温力も十分です。"),
        ),
    )

    // GET /mock/reviews/product/{productId}
    // simulate=error → 500エラー（サービスダウンのシミュレーション）
    // simulate=slow  → 5秒後にレスポンス（タイムアウトのシミュレーション）
    // simulate=none  → 正常にレビューを返す
    @GetMapping("/product/{productId}")
    fun getByProductId(
        @PathVariable productId: Long,
        @RequestParam(defaultValue = "none") simulate: String,
    ): Flux<Review> = when (simulate) {
        "error" -> Flux.error(ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Review service is down"))
        "slow"  -> Mono.delay(Duration.ofSeconds(5))
            .flatMapMany { Flux.fromIterable(reviews[productId] ?: emptyList()) }
        else    -> Flux.fromIterable(reviews[productId] ?: emptyList())
    }
}
