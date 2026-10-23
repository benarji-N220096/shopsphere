package com.shopsphere.auth.controller;

import com.shopsphere.auth.dto.GoogleAuthRequest;
import com.shopsphere.auth.dto.LoginRequest;
import com.shopsphere.auth.dto.LoginResponse;
import com.shopsphere.auth.dto.RegistrationRequest;
import com.shopsphere.auth.dto.RegistrationResponse;
import com.shopsphere.auth.service.AuthenticationService;
import com.shopsphere.auth.service.GoogleAuthService;
import com.shopsphere.auth.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;
    private GoogleAuthService googleAuthService;

    public AuthController(RegistrationService registrationService, AuthenticationService authenticationService) {
        this.registrationService = registrationService;
        this.authenticationService = authenticationService;
    }

    @Autowired
    public AuthController(
            RegistrationService registrationService,
            AuthenticationService authenticationService,
            GoogleAuthService googleAuthService
    ) {
        this.registrationService = registrationService;
        this.authenticationService = authenticationService;
        this.googleAuthService = googleAuthService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        RegistrationResponse response = registrationService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authenticationService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> loginWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        if (googleAuthService == null) {
            throw new IllegalStateException("Google authentication service is not configured");
        }
        LoginResponse response = googleAuthService.authenticateWithGoogle(request);
        return ResponseEntity.ok(response);
    }
}
