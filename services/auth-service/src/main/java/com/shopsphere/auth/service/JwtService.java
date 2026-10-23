package com.shopsphere.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Set;
import java.util.Base64;

@Service
public class JwtService {

    @Value("${shopsphere.security.jwt.secret}")
    private String secretKey;

    @Value("${shopsphere.security.jwt.expiration-ms}")
    private long jwtExpiration;

    private SecretKey getSignInKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String userId, String email, Set<String> roles) {
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }
}
