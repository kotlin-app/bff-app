package com.example.bff.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Tag(name = "Orders", description = "注文 API（order-service プロキシ）")
@RestController
@RequestMapping("/api/orders")
class OrderController(
    @Qualifier("orderWebClient") private val webClient: WebClient,
) {

    @Operation(summary = "注文作成", security = [SecurityRequirement(name = "BearerAuth")])
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrder(
        @RequestBody body: Map<String, Any>,
        auth: Authentication,
    ): Mono<Any> =
        webClient.post()
            .bodyValue(body + ("userId" to auth.name))
            .retrieve()
            .bodyToMono(Any::class.java)

    @Operation(summary = "自分の注文一覧", security = [SecurityRequirement(name = "BearerAuth")])
    @GetMapping("/my")
    fun getMyOrders(auth: Authentication): Mono<List<Any>> =
        webClient.get()
            .uri("/user/${auth.name}")
            .retrieve()
            .bodyToFlux(Any::class.java)
            .collectList()
}
