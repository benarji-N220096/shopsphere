package com.shopsphere.order.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentUserProviderTest {

    private CurrentUserProvider currentUserProvider;

    @BeforeEach
    void setUp() {
        currentUserProvider = new CurrentUserProvider();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserId_authenticatedJwt_returnsUserIdFromSub() {
        UUID expectedUserId = UUID.randomUUID();

        Jwt jwt = new Jwt(
                "mock-token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS384"),
                Map.of("sub", expectedUserId.toString(), "email", "test@example.com")
        );

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        UUID actualUserId = currentUserProvider.getCurrentUserId();
        assertEquals(expectedUserId, actualUserId);
    }

    @Test
    void getCurrentUserId_unauthenticated_throwsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> currentUserProvider.getCurrentUserId());
    }
}
