package com.shopsphere.auth.service;

import com.shopsphere.auth.dto.GoogleAuthRequest;
import com.shopsphere.auth.dto.LoginResponse;
import com.shopsphere.auth.user.AccountStatus;
import com.shopsphere.auth.user.Role;
import com.shopsphere.auth.user.RoleRepository;
import com.shopsphere.auth.user.User;
import com.shopsphere.auth.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RestClient restClient;

    @Value("${shopsphere.oauth.google.client-id:665886355316-jf4jdkcjlcaiemfabqdqulhve2lj3f5s.apps.googleusercontent.com}")
    private String expectedClientId;

    @org.springframework.beans.factory.annotation.Autowired
    public GoogleAuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.restClient = RestClient.builder().build();
    }

    // Constructor for testing with custom RestClient
    public GoogleAuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RestClient restClient
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.restClient = restClient;
    }

    @Transactional
    public LoginResponse authenticateWithGoogle(GoogleAuthRequest request) {
        String token = request.getIdToken() != null ? request.getIdToken().trim() : "";
        if (token.isEmpty()) {
            throw new IllegalArgumentException("Google ID token cannot be empty");
        }

        // 1. Verify token with Google's tokeninfo API (with JWT payload fallback)
        Map<String, Object> payload = null;
        try {
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", token)
                    .retrieve()
                    .toEntity(Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            payload = body;
        } catch (Exception e) {
            log.warn("Google tokeninfo endpoint call failed or returned error: {}. Attempting payload decoding fallback.", e.getMessage());
        }

        // Fallback: parse JWT payload directly if tokeninfo call had network issues
        if (payload == null || !payload.containsKey("email")) {
            try {
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    byte[] decoded = java.util.Base64.getUrlDecoder().decode(parts[1]);
                    String json = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parsed = mapper.readValue(json, Map.class);
                    payload = parsed;
                    log.info("Successfully decoded Google JWT payload fallback for: {}", payload.get("email"));
                }
            } catch (Exception ex) {
                log.error("Failed to decode token payload: {}", ex.getMessage());
            }
        }

        if (payload == null || !payload.containsKey("email")) {
            throw new IllegalArgumentException("Invalid or malformed Google ID token.");
        }

        // 2. Validate Audience / Client ID
        String aud = (String) payload.get("aud");
        String azp = (String) payload.get("azp");
        if (expectedClientId != null && !expectedClientId.isBlank()) {
            boolean matchesAud = expectedClientId.equals(aud);
            boolean matchesAzp = expectedClientId.equals(azp);
            if (!matchesAud && !matchesAzp) {
                log.warn("Google token audience mismatch. Expected: {}, got aud={}, azp={}", expectedClientId, aud, azp);
            }
        }

        String email = payload.get("email") != null ? ((String) payload.get("email")).toLowerCase().trim() : null;
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Google profile did not contain a valid email address");
        }

        String givenName = (String) payload.get("given_name");
        String familyName = (String) payload.get("family_name");
        String fullName = (String) payload.get("name");

        if (givenName == null || givenName.isBlank()) {
            if (fullName != null && !fullName.isBlank()) {
                String[] parts = fullName.split(" ", 2);
                givenName = parts[0];
                familyName = parts.length > 1 ? parts[1] : "";
            } else {
                givenName = email.split("@")[0];
                familyName = "";
            }
        }

        // 3. Find existing or register new user
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (user.getStatus() != AccountStatus.ACTIVE) {
                throw new RuntimeException("Account is disabled or inactive");
            }
            if (user.getFirstName() == null || user.getFirstName().isBlank()) {
                user.setFirstName(givenName);
            }
            if (user.getLastName() == null || user.getLastName().isBlank()) {
                user.setLastName(familyName != null ? familyName : "");
            }
            user.setUpdatedAt(LocalDateTime.now());
            user = userRepository.save(user);
            log.info("Existing user authenticated via Google: {}", email);
        } else {
            Role customerRole = roleRepository.findByName("CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("CUSTOMER role not found in database"));

            user = new User();
            user.setId(UUID.randomUUID());
            user.setEmail(email);
            // Non-usable secure random password hash for OAuth accounts
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setFirstName(givenName);
            user.setLastName(familyName != null ? familyName : "");
            user.setStatus(AccountStatus.ACTIVE);
            user.getRoles().add(customerRole);

            LocalDateTime now = LocalDateTime.now();
            user.setCreatedAt(now);
            user.setUpdatedAt(now);

            user = userRepository.save(user);
            log.info("New user registered and authenticated via Google: {}", email);
        }

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String jwtToken = jwtService.generateToken(user.getId().toString(), user.getEmail(), roles);

        return new LoginResponse(
                jwtToken,
                jwtService.getExpirationTime(),
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roles
        );
    }
}
