package com.example.bff.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
@AutoConfigureWebTestClient
class AuthControllerTest {

    @Autowired
    lateinit var client: WebTestClient

    @Test
    fun `正しい認証情報でログインするとJWTトークンが返る`() {
        client.post().uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"username":"user","password":"user123"}""")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.token").isNotEmpty
            .jsonPath("$.username").isEqualTo("user")
            .jsonPath("$.role").isEqualTo("USER")
            .jsonPath("$.expiresIn").isEqualTo(3600)
    }

    @Test
    fun `存在しないユーザーでログインすると401が返る`() {
        client.post().uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"username":"unknown","password":"pass"}""")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `パスワードが間違っている場合は401が返る`() {
        client.post().uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"username":"user","password":"wrong"}""")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `ADMINユーザーでログインするとADMINロールのトークンが返る`() {
        client.post().uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"username":"admin","password":"admin123"}""")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.role").isEqualTo("ADMIN")
    }
}
