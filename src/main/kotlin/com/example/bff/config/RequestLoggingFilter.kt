package com.example.bff.config

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

// 全リクエストのメソッド・パス・レスポンスステータスをログ出力するフィルター
@Component
class RequestLoggingFilter : WebFilter {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val start = System.currentTimeMillis()

        return chain.filter(exchange).doFinally {
            val status = exchange.response.statusCode?.value() ?: 0
            val elapsed = System.currentTimeMillis() - start
            log.info("method={} path={} status={} elapsed={}ms",
                request.method, request.path, status, elapsed)
        }
    }
}
