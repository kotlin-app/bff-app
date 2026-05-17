package com.example.bff.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

// 商品が見つからない場合にスローする例外（HTTP 404）
// ProductService で 404 レスポンスを受け取った際に変換される
@ResponseStatus(HttpStatus.NOT_FOUND)
class ProductNotFoundException(id: Long) : RuntimeException("Product not found: $id")

// 上流サービスが利用不能な場合にスローする例外（HTTP 503）
// タイムアウトや予期しないエラーが発生した際に ProductService で変換される
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
class ServiceUnavailableException(service: String) : RuntimeException("$service is unavailable")
