package com.example.bff.controller

import com.example.bff.service.ReviewService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Cache", description = "キャッシュ管理API（ADMINロール専用）")
@RestController
@RequestMapping("/api/cache")
class CacheController(private val reviewService: ReviewService) {

    @Operation(summary = "キャッシュ統計取得", description = "レビューキャッシュのヒット率・保持件数などを返します", security = [SecurityRequirement(name = "BearerAuth")])
    @ApiResponse(responseCode = "200", description = "統計情報返却")
    @ApiResponse(responseCode = "403", description = "ADMINロールが必要")
    @GetMapping("/stats")
    fun getStats() = reviewService.getCacheStats()

    @Operation(summary = "キャッシュクリア", description = "レビューキャッシュを全件削除します", security = [SecurityRequirement(name = "BearerAuth")])
    @ApiResponse(responseCode = "200", description = "クリア完了")
    @ApiResponse(responseCode = "403", description = "ADMINロールが必要")
    @DeleteMapping
    fun clearCache(): Map<String, String> {
        reviewService.clearCache()
        return mapOf("message" to "Cache cleared")
    }
}
