package com.example.bff

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.Date

// テスト用JWTトークン生成ヘルパー
// application.yml の jwt.secret と同じ値を使う
object TestJwtHelper {
    private val secret = "bff-secret-key-must-be-at-least-256-bits-long-for-hs256"
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateToken(username: String, role: String): String =
        Jwts.builder()
            .subject(username)
            .claim("role", role)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3600_000))
            .signWith(key)
            .compact()

    val userToken: String get() = generateToken("user", "USER")
    val adminToken: String get() = generateToken("admin", "ADMIN")
}
