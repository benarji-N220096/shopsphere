package com.shopsphere.cart.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Component
public class CurrentUserProvider {

    /**
     * Extracts the current user ID.
     * - For client requests: strictly from the validated JWT SecurityContext (claims 'sub').
     *   Untrusted X-User-Id headers are ignored.
     * - For internal service calls (authenticated with ROLE_INTERNAL_SERVICE): from X-User-Id header.
     */
    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                String sub = jwt.getSubject();
                if (sub != null && !sub.isBlank()) {
                    return UUID.fromString(sub);
                }
            }

            boolean isInternal = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_INTERNAL_SERVICE".equals(a.getAuthority()));
            if (isInternal) {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    String headerUserId = request.getHeader("X-User-Id");
                    if (headerUserId != null && !headerUserId.isBlank()) {
                        return UUID.fromString(headerUserId);
                    }
                }
                throw new IllegalArgumentException("X-User-Id header is required for internal service calls");
            }
        }
        throw new IllegalStateException("User context not found. Authenticated JWT or internal service credentials required.");
    }
}
