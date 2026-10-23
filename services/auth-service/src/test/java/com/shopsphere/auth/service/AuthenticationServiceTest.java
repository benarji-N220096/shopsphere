package com.shopsphere.auth.service;

import com.shopsphere.auth.dto.LoginRequest;
import com.shopsphere.auth.dto.LoginResponse;
import com.shopsphere.auth.user.AccountStatus;
import com.shopsphere.auth.user.Role;
import com.shopsphere.auth.user.User;
import com.shopsphere.auth.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authenticationService = new AuthenticationService(userRepository, authenticationManager, passwordEncoder, jwtService);
    }

    @Test
    void authenticate_validCredentials_returnsLoginResponseWithJwt() {
        LoginRequest request = new LoginRequest("user@example.com", "validPassword");
        UUID userId = UUID.randomUUID();

        Role role = new Role();
        role.setName("CUSTOMER");

        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setStatus(AccountStatus.ACTIVE);
        user.setRoles(Set.of(role));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(eq(userId.toString()), eq("user@example.com"), any())).thenReturn("mock.jwt.token");
        when(jwtService.getExpirationTime()).thenReturn(3600000L);

        LoginResponse response = authenticationService.authenticate(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getAccessToken());
        assertEquals(userId.toString(), response.getId());
        assertEquals("user@example.com", response.getEmail());
        assertTrue(response.getRoles().contains("CUSTOMER"));
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void authenticate_invalidCredentials_throwsBadCredentialsException() {
        LoginRequest request = new LoginRequest("user@example.com", "wrongPassword");
        doThrow(new BadCredentialsException("Invalid username or password"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(BadCredentialsException.class, () -> authenticationService.authenticate(request));
    }
}
