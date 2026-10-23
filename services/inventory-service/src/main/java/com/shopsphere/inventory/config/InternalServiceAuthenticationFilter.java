package com.shopsphere.inventory.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Validates internal service communication credentials.
 * If X-Internal-Secret matches the configured secret, sets an authenticated
 * SecurityContext with ROLE_INTERNAL_SERVICE.
 */
@Component
public class InternalServiceAuthenticationFilter extends OncePerRequestFilter {

    private final String expectedSecret;

    public InternalServiceAuthenticationFilter(
            @Value("${shopsphere.security.internal.secret:shopsphere-internal-service-secret-key-384}") String expectedSecret) {
        this.expectedSecret = expectedSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String secret = request.getHeader("X-Internal-Secret");
        if (secret != null && !secret.isBlank()) {
            if (expectedSecret.equals(secret)) {
                String serviceName = request.getHeader("X-Internal-Service");
                if (serviceName == null || serviceName.isBlank()) {
                    serviceName = "internal-service";
                }
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        serviceName,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Invalid internal service secret\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
