package com.example.bff.config

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.util.Date

// JWTトークンの生成・検証・情報取得を担うユーティリティクラス
// ログイン時のトークン発行と、リクエストごとのトークン検証で使用する
@Component
class JwtUtil(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration-ms}") private val expirationMs: Long,
) {
    // シークレットキーから HMAC-SHA 署名キーを生成する
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    // ユーザー名とロールを埋め込んだ署名済みJWTトークンを生成する
    fun generateToken(username: String, role: String): String =
        Jwts.builder()
            .subject(username)
            .claim("role", role)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact()

    // トークンの署名と有効期限を検証する（改ざん・期限切れの場合は false を返す）
    fun validateToken(token: String): Boolean = runCatching {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
        true
    }.getOrDefault(false)

    // トークンからユーザー名・ロールを取り出し、Spring Security の Authentication オブジェクトを生成する
    fun getAuthentication(token: String): Authentication {
        val claims = Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload
        val role = claims["role"] as String
        return UsernamePasswordAuthenticationToken(
            claims.subject,
            null,
            listOf(SimpleGrantedAuthority("ROLE_$role")),
        )
    }

    // トークンからユーザー名のみを取り出す
    fun getUsername(token: String): String =
        Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload.subject
}
