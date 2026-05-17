package com.example.bff.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@Tag(name = "Wishlist", description = "お気に入り商品 API（Redis Set）")
@RestController
@RequestMapping("/api/wishlist")
class WishlistController(private val redis: ReactiveStringRedisTemplate) {

    private fun key(username: String) = "wishlist:$username"

    @Operation(summary = "お気に入り一覧取得", security = [SecurityRequirement(name = "BearerAuth")])
    @GetMapping
    fun getWishlist(auth: Authentication): Mono<Set<String>> =
        redis.opsForSet().members(key(auth.name)).collect(java.util.stream.Collectors.toSet())

    @Operation(summary = "お気に入り追加", security = [SecurityRequirement(name = "BearerAuth")])
    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun addToWishlist(@PathVariable productId: Long, auth: Authentication): Mono<Long> =
        redis.opsForSet().add(key(auth.name), productId.toString())

    @Operation(summary = "お気に入り削除", security = [SecurityRequirement(name = "BearerAuth")])
    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeFromWishlist(@PathVariable productId: Long, auth: Authentication): Mono<Long> =
        redis.opsForSet().remove(key(auth.name), productId.toString())
}
