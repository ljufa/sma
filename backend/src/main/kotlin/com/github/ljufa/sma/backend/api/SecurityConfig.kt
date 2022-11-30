package com.github.ljufa.sma.backend.api

import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource


@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig {

    @Bean
    @Throws(Exception::class)
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf().disable()
            .build()
    }

    @Bean
    fun corsConfiguration(): CorsConfigurationSource {
        val corsConfig = CorsConfiguration()
        corsConfig.applyPermitDefaultValues()
        corsConfig.allowCredentials = true
        corsConfig.addAllowedMethod("GET")
        corsConfig.addAllowedMethod("POST")
        corsConfig.allowedOrigins = listOf("http://localhost:8000", "https://sma.dlj.freemyip.com")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", corsConfig)
        return source
    }

    @Bean
    fun corsWebFilter(): CorsWebFilter? {
        return CorsWebFilter(corsConfiguration())
    }
}
