package com.example.bff.controller

import com.example.bff.model.*
import com.example.bff.service.ProductService
import com.example.bff.service.ReviewService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@Tag(name = "Products", description = "BFF商品API。X-Client-Typeヘッダーで web/mobile を切り替え可能。")
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

    @Operation(
        summary = "商品一覧取得",
        description = "X-Client-Type: web → 全フィールド、mobile → 軽量レスポンス",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @ApiResponse(responseCode = "200", description = "取得成功")
    @ApiResponse(responseCode = "401", description = "認証が必要")
    @GetMapping
    fun getProducts(
        @Parameter(description = "クライアント種別 (web または mobile)", example = "web")
        @RequestHeader("X-Client-Type", defaultValue = "web") clientType: String
    ): Any = when (clientType.lowercase()) {
        "mobile" -> mockProducts.map { it.toMobileResponse() }
        else -> mockProducts.map { it.toWebResponse() }
    }

    @Operation(
        summary = "商品詳細取得（レビュー付き）",
        description = "商品サービスとレビューサービスを並列呼び出しして集約。simulate=error でレビューフォールバック動作を確認できます。",
        security = [SecurityRequirement(name = "BearerAuth")]
    )
    @ApiResponse(responseCode = "200", description = "取得成功")
    @ApiResponse(responseCode = "401", description = "認証が必要")
    @ApiResponse(responseCode = "404", description = "商品が存在しない")
    @ApiResponse(responseCode = "503", description = "商品サービスが利用不可")
    @GetMapping("/{id}")
    fun getProductDetail(
        @Parameter(description = "商品ID") @PathVariable id: Long,
        @Parameter(description = "クライアント種別 (web または mobile)", example = "web")
        @RequestHeader("X-Client-Type", defaultValue = "web") clientType: String,
        @Parameter(description = "障害シミュレーション (none/error/slow)", example = "none")
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

    private fun Product.toWebResponse() = ProductWebResponse(id, name, price, description, stock, category)
    private fun Product.toMobileResponse() = ProductMobileResponse(id, name, price, stock)

    private fun Product.toDetailWebResponse(reviews: List<Review>, avg: Double) =
        ProductDetailWebResponse(id, name, price, description, stock, category, reviews, avg)

    private fun Product.toDetailMobileResponse(reviews: List<Review>, avg: Double) =
        ProductDetailMobileResponse(id, name, price, stock, reviews.firstOrNull(), avg)
}
