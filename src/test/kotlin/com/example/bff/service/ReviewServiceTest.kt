package com.example.bff.service

import com.example.bff.model.Review
import com.github.benmanes.caffeine.cache.Caffeine
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier
import java.util.concurrent.TimeUnit

class ReviewServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var reviewService: ReviewService

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/reviews").toString())
            .build()
        val cache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build<Long, List<Review>>()
        reviewService = ReviewService(webClient, cache)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `レビューが正常に返る`() {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""[{"id":1,"productId":1,"userName":"田中さん","rating":5,"comment":"最高"}]""")
                .addHeader("Content-Type", "application/json")
        )

        StepVerifier.create(reviewService.getReviews(1L))
            .expectNextMatches { it.size == 1 && it[0].userName == "田中さん" }
            .verifyComplete()
    }

    @Test
    fun `2回目のリクエストはキャッシュから返る`() {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""[{"id":1,"productId":1,"userName":"田中さん","rating":5,"comment":"最高"}]""")
                .addHeader("Content-Type", "application/json")
        )

        // 1回目（キャッシュミス）
        reviewService.getReviews(1L).block()

        // 2回目（キャッシュヒット：MockWebServerへのリクエストが発生しない）
        StepVerifier.create(reviewService.getReviews(1L))
            .expectNextMatches { it.size == 1 }
            .verifyComplete()

        // MockWebServerへのリクエストが1回だけであることを確認
        assert(mockWebServer.requestCount == 1)
    }

    @Test
    fun `サービスエラー時は空リストを返す（フォールバック）`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        StepVerifier.create(reviewService.getReviews(99L))
            .expectNextMatches { it.isEmpty() }
            .verifyComplete()
    }

    @Test
    fun `キャッシュクリア後は再度バックエンドを呼び出す`() {
        repeat(2) {
            mockWebServer.enqueue(
                MockResponse()
                    .setBody("""[{"id":1,"productId":1,"userName":"田中さん","rating":5,"comment":"最高"}]""")
                    .addHeader("Content-Type", "application/json")
            )
        }

        reviewService.getReviews(1L).block()
        reviewService.clearCache()
        reviewService.getReviews(1L).block()

        assert(mockWebServer.requestCount == 2)
    }
}
