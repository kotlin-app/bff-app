package com.example.bff.controller

import com.example.bff.TestJwtHelper
import com.example.bff.model.Product
import com.example.bff.model.Review
import com.example.bff.service.ProductService
import com.example.bff.service.ReviewService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@SpringBootTest
@AutoConfigureWebTestClient
class ProductControllerTest {

    @Autowired
    lateinit var client: WebTestClient

    @MockkBean
    lateinit var productService: ProductService

    @MockkBean
    lateinit var reviewService: ReviewService

    private val token = "Bearer ${TestJwtHelper.userToken}"

    private val sampleProduct = Product(1, "コーヒーメーカー", 12000, "説明文", 50, "家電")
    private val sampleReviews = listOf(
        Review(1, 1, "田中さん", 5, "最高です"),
        Review(2, 1, "佐藤さん", 4, "良いです"),
    )

    @Test
    fun `認証なしで商品一覧にアクセスすると401が返る`() {
        client.get().uri("/api/products")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `Web用レスポンスにはdescriptionとcategoryが含まれる`() {
        client.get().uri("/api/products")
            .header("Authorization", token)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[0].description").isNotEmpty
            .jsonPath("$[0].category").isNotEmpty
    }

    @Test
    fun `モバイル用レスポンスにはdescriptionとcategoryが含まれない`() {
        client.get().uri("/api/products")
            .header("Authorization", token)
            .header("X-Client-Type", "mobile")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[0].description").doesNotExist()
            .jsonPath("$[0].category").doesNotExist()
    }

    @Test
    fun `商品詳細はレビューと集約して返る`() {
        every { productService.getProduct(1L) } returns Mono.just(sampleProduct)
        every { reviewService.getReviews(1L, "none") } returns Mono.just(sampleReviews)

        client.get().uri("/api/products/1")
            .header("Authorization", token)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.name").isEqualTo("コーヒーメーカー")
            .jsonPath("$.reviews").isArray
            .jsonPath("$.reviews.length()").isEqualTo(2)
            .jsonPath("$.averageRating").isEqualTo(4.5)
    }

    @Test
    fun `モバイル用商品詳細は上位1件のレビューのみ返る`() {
        every { productService.getProduct(1L) } returns Mono.just(sampleProduct)
        every { reviewService.getReviews(1L, "none") } returns Mono.just(sampleReviews)

        client.get().uri("/api/products/1")
            .header("Authorization", token)
            .header("X-Client-Type", "mobile")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.topReview").isNotEmpty
            .jsonPath("$.description").doesNotExist()
    }

    @Test
    fun `レビューサービスが失敗しても商品は返る`() {
        every { productService.getProduct(1L) } returns Mono.just(sampleProduct)
        every { reviewService.getReviews(1L, "none") } returns Mono.just(emptyList())

        client.get().uri("/api/products/1")
            .header("Authorization", token)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.name").isEqualTo("コーヒーメーカー")
            .jsonPath("$.reviews.length()").isEqualTo(0)
            .jsonPath("$.averageRating").isEqualTo(0.0)
    }
}
