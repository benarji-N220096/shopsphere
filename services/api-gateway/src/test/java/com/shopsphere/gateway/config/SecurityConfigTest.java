package com.shopsphere.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    private final String testSecret = "8f2a1b9c8e4d3f2a1b9c8e4d3f2a1b9c8e4d3f2a1b9c8e4d3f2a1b9c8e4d3f2a";

    @Test
    void reactiveJwtDecoder_invalidToken_rejectsWithException() {
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "jwtSecret", testSecret);

        ReactiveJwtDecoder decoder = config.reactiveJwtDecoder();
        assertNotNull(decoder);

        assertThrows(BadJwtException.class, () -> {
            decoder.decode("invalid.jwt.token").block(Duration.ofSeconds(5));
        });
    }
}
