package com.example.bff.exception

import java.time.LocalDateTime

// APIエラー時にクライアントへ返す統一レスポンス形式
// すべてのエラーをこの形式で返すことでクライアント側の処理を統一できる
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
)
