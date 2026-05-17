package com.example.bff.controller.mock

import com.example.bff.model.Product
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

// 商品マイクロサービスを模倣するモックコントローラー
// 実際のマイクロサービスが用意できるまでの代替として使用する
// 本番時はこのコントローラーを削除し、WebClientConfig の baseUrl を実際のサービスURLに切り替える
@RestController
@RequestMapping("/mock/products")
class MockProductController {

    // インメモリの商品データ（実際は DB や外部サービスから取得する）
    private val products = mapOf(
        1L to Product(1, "コーヒーメーカー", 12000, "高品質なコーヒーメーカーです。豊かな香りを楽しめます。", 50, "家電"),
        2L to Product(2, "ワイヤレスイヤホン", 8000, "ノイズキャンセリング対応の高音質イヤホン。最大30時間再生。", 100, "家電"),
        3L to Product(3, "水筒", 3000, "保温・保冷対応の500ml水筒。軽量設計でアウトドアにも最適。", 200, "日用品"),
    )

    // GET /mock/products → 全件返す
    @GetMapping
    fun getAll(): List<Product> = products.values.toList()

    // GET /mock/products/{id} → 該当商品を返す（存在しない場合は404）
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): Product =
        products[id] ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: $id")
}
