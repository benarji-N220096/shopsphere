package com.shopsphere.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${shopsphere.security.jwt.secret:8f2a1b9c8e4d3f2a1b9c8e4d3f2a1b9c8e4d3f2a1b9c8e4d3f2a1b9c8e4d3f2a}")
    private String jwtSecret;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                // Public auth endpoints
                .pathMatchers("/api/v1/auth/**").permitAll()
                // Public product browsing
                .pathMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                // CORS preflight requests
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                // Actuator endpoints
                .pathMatchers("/actuator/**").permitAll()
                // Internal endpoints that must NEVER be called through public API gateway
                .pathMatchers("/api/v1/orders/*/recover", "/api/v1/orders/recover-pending").denyAll()
                .pathMatchers("/api/v1/inventory/reservations/**").denyAll()
                // All other business endpoints require authentication
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder()))
            );

        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA384");
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS384)
                .build();
    }
}
