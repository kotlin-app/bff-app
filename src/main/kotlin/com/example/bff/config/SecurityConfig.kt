package com.example.bff.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint

// Spring Security の設定クラス（WebFlux対応）
// 認証が不要なパスと必要なパスを定義し、JwtAuthFilter を認証フェーズに組み込む
@Configuration
@EnableWebFluxSecurity
class SecurityConfig(private val jwtAuthFilter: JwtAuthFilter) {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }      // REST APIのためCSRF保護を無効化
            .httpBasic { it.disable() } // Basic認証を無効化
            .formLogin { it.disable() } // フォームログインを無効化
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/api/auth/**").permitAll()          // ログインは認証不要
                    .pathMatchers("/mock/**").permitAll()               // モックサービスは認証不要
                    .pathMatchers("/actuator/**").permitAll()           // ヘルスチェックは認証不要
                    .pathMatchers("/api/cache/**").hasRole("ADMIN")    // キャッシュ管理はADMINロールのみ
                    .anyExchange().authenticated()                     // それ以外はすべて認証必須
            }
            // 未認証アクセス時は 401 を返す（デフォルトのリダイレクトを抑制するため明示的に設定）
            .exceptionHandling {
                it.authenticationEntryPoint(HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
            }
            // JwtAuthFilter を Spring Security の認証フェーズに割り込ませる
            .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
}
