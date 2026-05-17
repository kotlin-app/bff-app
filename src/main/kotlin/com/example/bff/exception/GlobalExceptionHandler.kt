package com.example.bff.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

// アプリケーション全体の例外を一元ハンドリングするクラス
// 各コントローラーで個別に try-catch を書かず、例外の種類に応じて統一フォーマットでエラーを返す
@RestControllerAdvice
class GlobalExceptionHandler {

    // 商品が見つからない → 404 Not Found
    @ExceptionHandler(ProductNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: ProductNotFoundException) = ErrorResponse(
        status = 404,
        error = "Not Found",
        message = ex.message ?: "Resource not found",
    )

    // 上流サービスが利用不能 → 503 Service Unavailable
    @ExceptionHandler(ServiceUnavailableException::class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun handleServiceUnavailable(ex: ServiceUnavailableException) = ErrorResponse(
        status = 503,
        error = "Service Unavailable",
        message = ex.message ?: "Upstream service is unavailable",
    )
}
