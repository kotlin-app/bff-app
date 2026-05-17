package com.example.bff.config

import org.reactivestreams.Publisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Profile("keycloak")
@Configuration
@EnableWebFluxSecurity
class KeycloakSecurityConfig {

    @Bean
    fun keycloakSecurityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .cors {}
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .authorizeExchange { ex ->
                ex
                    .pathMatchers("/actuator/**").permitAll()
                    .pathMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/webjars/**").permitAll()
                    .pathMatchers("/api/cache/**").hasRole("ADMIN")
                    .anyExchange().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(keycloakJwtConverter())
                }
            }
            .build()

    private fun keycloakJwtConverter(): Converter<Jwt, Mono<AbstractAuthenticationToken>> =
        Converter { jwt ->
            val authorities = extractRealmRoles(jwt)
            val username = jwt.getClaimAsString("preferred_username") ?: jwt.subject
            Mono.just(JwtAuthenticationToken(jwt, authorities, username))
        }

    @Suppress("UNCHECKED_CAST")
    private fun extractRealmRoles(jwt: Jwt): Collection<GrantedAuthority> {
        val realmAccess = jwt.claims["realm_access"] as? Map<*, *> ?: return emptyList()
        val roles = realmAccess["roles"] as? List<*> ?: return emptyList()
        return roles.filterIsInstance<String>().map { SimpleGrantedAuthority("ROLE_${it.uppercase()}") }
    }
}
