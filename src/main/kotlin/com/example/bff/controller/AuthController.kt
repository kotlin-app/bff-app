package com.example.bff.controller

import com.example.bff.config.JwtUtil
import com.example.bff.model.LoginRequest
import com.example.bff.model.LoginResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

// 認証エンドポイントを提供するコントローラー
// ログイン成功時にJWTトークンを発行する
// 実際のプロダクションではユーザーサービスに問い合わせてユーザーを検証する
@RestController
@RequestMapping("/api/auth")
class AuthController(private val jwtUtil: JwtUtil) {

    // モックユーザー（実際はユーザーサービスに問い合わせる）
    private val users = mapOf(
        "admin" to Pair("admin123", "ADMIN"),
        "user"  to Pair("user123",  "USER"),
    )

    // POST /api/auth/login
    // ユーザー名・パスワードを受け取り、認証成功時にJWTトークンを返す
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): LoginResponse {
        val (password, role) = users[request.username]
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")

        if (password != request.password) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        }

        val token = jwtUtil.generateToken(request.username, role)
        return LoginResponse(
            token = token,
            username = request.username,
            role = role,
            expiresIn = 3600,
        )
    }
}
