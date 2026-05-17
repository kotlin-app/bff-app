package com.example.bff.model

// 商品サービスから取得する商品の内部表現
data class Product(
    val id: Long,
    val name: String,
    val price: Int,
    val description: String,
    val stock: Int,
    val category: String,
)

// レビューサービスから取得するレビューの内部表現
data class Review(
    val id: Long,
    val productId: Long,
    val userName: String,
    val rating: Int,
    val comment: String,
)

// --- クライアント向けレスポンス型 ---
// 内部モデルとレスポンス型を分けることで、サービス仕様変更の影響をBFF層に閉じ込める

// Web用 商品詳細：全フィールド + 全レビュー一覧
data class ProductDetailWebResponse(
    val id: Long,
    val name: String,
    val price: Int,
    val description: String,
    val stock: Int,
    val category: String,
    val reviews: List<Review>,
    val averageRating: Double,
)

// モバイル用 商品詳細：軽量フィールド + 上位1件のレビューのみ（通信量削減）
data class ProductDetailMobileResponse(
    val id: Long,
    val name: String,
    val price: Int,
    val stock: Int,
    val topReview: Review?,
    val averageRating: Double,
)

// Web用 商品一覧：全フィールドを返す
data class ProductWebResponse(
    val id: Long,
    val name: String,
    val price: Int,
    val description: String,
    val stock: Int,
    val category: String,
)

// モバイル用 商品一覧：表示に必要な最小限のフィールドのみ返す
data class ProductMobileResponse(
    val id: Long,
    val name: String,
    val price: Int,
    val stock: Int,
)
