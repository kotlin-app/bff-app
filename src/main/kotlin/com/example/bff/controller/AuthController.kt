package com.example.bff.controller

import com.example.bff.config.JwtUtil
import com.example.bff.model.LoginRequest
import com.example.bff.model.LoginResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Auth", description = "認証API（JWTトークン発行）")
@RestController
@RequestMapping("/api/auth")
class AuthController(private val jwtUtil: JwtUtil) {

    private val users = mapOf(
        "admin" to Pair("admin123", "ADMIN"),
        "user"  to Pair("user123",  "USER"),
    )

    @Operation(summary = "ログイン", description = "ユーザー名・パスワードを送信してJWTトークンを取得します。admin/admin123 または user/user123 が使用できます。")
    @ApiResponse(responseCode = "200", description = "認証成功・JWTトークン返却")
    @ApiResponse(responseCode = "401", description = "認証失敗")
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
