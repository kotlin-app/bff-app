package com.example.bff.controller

import com.example.bff.model.*
import com.example.bff.service.ProductService
import com.example.bff.service.ReviewService
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

// BFFの中核となる商品APIコントローラー
// リクエストヘッダー "X-Client-Type" に応じて Web用・モバイル用と異なるレスポンスを返す
// 商品詳細では商品サービスとレビューサービスを並列で呼び出し、1つのレスポンスに集約する
@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService,
    private val reviewService: ReviewService,
) {

    private val mockProducts = listOf(
        Product(1, "コーヒーメーカー", 12000, "高品質なコーヒーメーカーです。", 50, "家電"),
        Product(2, "ワイヤレスイヤホン", 8000, "ノイズキャンセリング対応の高音質イヤホン。", 100, "家電"),
        Product(3, "水筒", 3000, "保温・保冷対応の500ml水筒。", 200, "日用品"),
    )

    // GET /api/products
    // X-Client-Type: web    → 全フィールドを返す（説明・カテゴリあり）
    // X-Client-Type: mobile → 必要最低限のフィールドのみ返す（通信量削減）
    @GetMapping
    fun getProducts(
        @RequestHeader("X-Client-Type", defaultValue = "web") clientType: String
    ): Any = when (clientType.lowercase()) {
        "mobile" -> mockProducts.map { it.toMobileResponse() }
        else -> mockProducts.map { it.toWebResponse() }
    }

    // GET /api/products/{id}
    // 商品サービスとレビューサービスを Mono.zip で並列呼び出しし、結果を1つにまとめて返す
    // simulate=error → レビューサービスがエラーを返す（商品は正常に返ることを確認）
    // simulate=slow  → レビューサービスが遅延（タイムアウトして空リストになることを確認）
    @GetMapping("/{id}")
    fun getProductDetail(
        @PathVariable id: Long,
        @RequestHeader("X-Client-Type", defaultValue = "web") clientType: String,
        @RequestParam(defaultValue = "none") simulate: String,
    ): Mono<Any> =
        Mono.zip(
            productService.getProduct(id),
            reviewService.getReviews(id, simulate),
        ).map { tuple ->
            val product = tuple.t1
            val reviews = tuple.t2
            val avgRating = if (reviews.isEmpty()) 0.0
                            else reviews.map { it.rating }.average()
            when (clientType.lowercase()) {
                "mobile" -> product.toDetailMobileResponse(reviews, avgRating)
                else -> product.toDetailWebResponse(reviews, avgRating)
            }
        }

    // 拡張関数：Product を各レスポンス型に変換する
    private fun Product.toWebResponse() = ProductWebResponse(id, name, price, description, stock, category)
    private fun Product.toMobileResponse() = ProductMobileResponse(id, name, price, stock)

    private fun Product.toDetailWebResponse(reviews: List<Review>, avg: Double) =
        ProductDetailWebResponse(id, name, price, description, stock, category, reviews, avg)

    private fun Product.toDetailMobileResponse(reviews: List<Review>, avg: Double) =
        ProductDetailMobileResponse(id, name, price, stock, reviews.firstOrNull(), avg)
}
