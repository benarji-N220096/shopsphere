package com.shopsphere.auth.service;

import com.shopsphere.auth.dto.RegistrationRequest;
import com.shopsphere.auth.dto.RegistrationResponse;
import com.shopsphere.auth.exception.EmailAlreadyExistsException;
import com.shopsphere.auth.exception.RoleNotFoundException;
import com.shopsphere.auth.user.AccountStatus;
import com.shopsphere.auth.user.Role;
import com.shopsphere.auth.user.RoleRepository;
import com.shopsphere.auth.user.User;
import com.shopsphere.auth.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        registrationService = new RegistrationService(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void testSuccessfulRegistration() {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com ");
        request.setPassword("SecurePassword123!");
        request.setFirstName("John ");
        request.setLastName("Doe");

        Role customerRole = new Role(UUID.randomUUID(), "CUSTOMER");
        
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationResponse response = registrationService.register(request);

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
        assertTrue(response.getRoles().contains("CUSTOMER"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertNotEquals("SecurePassword123!", savedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches("SecurePassword123!", savedUser.getPasswordHash()));
        assertTrue(savedUser.getRoles().contains(customerRole));
    }

    @Test
    void testDuplicateEmailRegistration() {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(EmailAlreadyExistsException.class, () -> registrationService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testMissingCustomerRole() {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("test@example.com");
        
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.empty());

        assertThrows(RoleNotFoundException.class, () -> registrationService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }
}
