package com.example.bff.model

// ログインリクエストのボディ
data class LoginRequest(
    val username: String,
    val password: String,
)

// ログイン成功時のレスポンス
// token をクライアントが保持し、以降のリクエストの Authorization ヘッダーに付与する
data class LoginResponse(
    val token: String,
    val username: String,
    val role: String,
    val expiresIn: Long, // トークンの有効期限（秒）
)
