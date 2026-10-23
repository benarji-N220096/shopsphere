package com.shopsphere.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String testSecret = "8f2a1b9c8e4d3f2a1b9c8e4d3f2a1b9c8e4d3f2a1b9c8e4d3f2a1b9c8e4d3f2a";
    private final long expirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", testSecret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", expirationMs);
    }

    @Test
    void generateToken_containsExpectedClaims() {
        String userId = UUID.randomUUID().toString();
        String email = "customer@example.com";
        Set<String> roles = Set.of("CUSTOMER");

        String token = jwtService.generateToken(userId, email, roles);

        assertNotNull(token);

        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(testSecret));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(userId, claims.getSubject());
        assertEquals(email, claims.get("email"));
        
        List<?> tokenRoles = (List<?>) claims.get("roles");
        assertTrue(tokenRoles.contains("CUSTOMER"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }
}
