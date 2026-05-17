package com.example.bff.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Tag(name = "Admin", description = "管理者専用 API（ADMIN ロール必須）")
@RestController
@RequestMapping("/api/admin/products")
class AdminController(
    @Qualifier("productWebClient") private val webClient: WebClient,
    private val cbFactory: ReactiveCircuitBreakerFactory<*, *>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(summary = "商品一覧（管理）", security = [SecurityRequirement(name = "BearerAuth")])
    @GetMapping
    fun listAll(): Mono<List<Any>> = run(webClient.get().retrieve().bodyToFlux(Any::class.java).collectList())

    @Operation(summary = "商品作成", security = [SecurityRequirement(name = "BearerAuth")])
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody body: Map<String, Any>): Mono<Any> =
        run(webClient.post().bodyValue(body).retrieve().bodyToMono(Any::class.java))

    @Operation(summary = "商品更新", security = [SecurityRequirement(name = "BearerAuth")])
    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody body: Map<String, Any>): Mono<Any> =
        run(webClient.put().uri("/$id").bodyValue(body).retrieve().bodyToMono(Any::class.java))

    @Operation(summary = "在庫更新", security = [SecurityRequirement(name = "BearerAuth")])
    @PatchMapping("/{id}/stock")
    fun updateStock(@PathVariable id: Long, @RequestBody body: Map<String, Int>): Mono<Any> =
        run(webClient.patch().uri("/$id/stock").bodyValue(body).retrieve().bodyToMono(Any::class.java))

    @Operation(summary = "商品削除", security = [SecurityRequirement(name = "BearerAuth")])
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long): Mono<Void> =
        run(webClient.delete().uri("/$id").retrieve().bodyToMono(Void::class.java))

    private fun <T> run(call: Mono<T>): Mono<T> =
        cbFactory.create("product-service").run(call) { ex ->
            log.error("product-service circuit open (admin): {}", ex.message)
            Mono.error(RuntimeException("商品サービスが利用できません"))
        }
}
