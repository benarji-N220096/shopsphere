package com.shopsphere.auth.controller;

import com.shopsphere.auth.dto.RegistrationRequest;
import com.shopsphere.auth.dto.RegistrationResponse;
import com.shopsphere.auth.exception.EmailAlreadyExistsException;
import com.shopsphere.auth.service.RegistrationService;
import com.shopsphere.auth.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private RegistrationService registrationService;
    private AuthenticationService authenticationService;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        registrationService = mock(RegistrationService.class);
        authenticationService = mock(AuthenticationService.class);
        authController = new AuthController(registrationService, authenticationService);
    }

    @Test
    void register_Success() {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePassword123!");
        request.setFirstName("John");
        request.setLastName("Doe");

        RegistrationResponse response = new RegistrationResponse();
        response.setEmail("test@example.com");
        response.setFirstName("John");

        when(registrationService.register(any(RegistrationRequest.class))).thenReturn(response);

        ResponseEntity<RegistrationResponse> result = authController.register(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("test@example.com", result.getBody().getEmail());
    }

    @Test
    void register_DuplicateEmail() {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");

        when(registrationService.register(any(RegistrationRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("Email is already in use"));

        assertThrows(EmailAlreadyExistsException.class, () -> authController.register(request));
    }
}
