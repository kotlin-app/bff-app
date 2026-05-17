package com.example.bff.service

import com.example.bff.exception.ProductNotFoundException
import com.example.bff.exception.ServiceUnavailableException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

class ProductServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var productService: ProductService

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/products").toString())
            .build()
        productService = ProductService(webClient)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `商品が存在する場合は正常に返る`() {
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"id":1,"name":"コーヒーメーカー","price":12000,"description":"説明","stock":50,"category":"家電"}""")
                .addHeader("Content-Type", "application/json")
        )

        StepVerifier.create(productService.getProduct(1L))
            .expectNextMatches { it.id == 1L && it.name == "コーヒーメーカー" }
            .verifyComplete()
    }

    @Test
    fun `商品が存在しない場合はProductNotFoundExceptionが返る`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))

        StepVerifier.create(productService.getProduct(999L))
            .expectError(ProductNotFoundException::class.java)
            .verify()
    }

    @Test
    fun `サービスが500エラーを返す場合はServiceUnavailableExceptionになる`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        StepVerifier.create(productService.getProduct(1L))
            .expectError(ServiceUnavailableException::class.java)
            .verify()
    }
}
