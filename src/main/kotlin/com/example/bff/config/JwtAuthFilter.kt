package com.example.bff.config

import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

// 全リクエストに対してJWTトークンを検証するフィルター
// Authorization ヘッダーからトークンを取り出し、有効であれば SecurityContext にセットする
// トークンがない・無効な場合はそのまま次のフィルターに処理を渡す（認証が必要なパスはSecurityConfigで拒否される）
@Component
class JwtAuthFilter(private val jwtUtil: JwtUtil) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val token = extractToken(exchange) ?: return chain.filter(exchange)

        if (!jwtUtil.validateToken(token)) return chain.filter(exchange)

        // 検証済みのトークンから Authentication を生成し、リアクティブな SecurityContext に登録する
        val authentication = jwtUtil.getAuthentication(token)
        return chain.filter(exchange)
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
    }

    // "Authorization: Bearer {token}" ヘッダーからトークン文字列だけを抽出する
    private fun extractToken(exchange: ServerWebExchange): String? =
        exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith("Bearer ") }
            ?.substring(7)
}
