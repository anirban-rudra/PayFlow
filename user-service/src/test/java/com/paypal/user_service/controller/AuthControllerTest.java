package com.paypal.user_service.controller;

import com.paypal.user_service.dto.AuthMessageResponse;
import com.paypal.user_service.dto.JwtResponse;
import com.paypal.user_service.dto.LoginRequest;
import com.paypal.user_service.dto.SignupRequest;
import com.paypal.user_service.entity.User;
import com.paypal.user_service.repository.UserRepository;
import com.paypal.user_service.service.UserService;
import com.paypal.user_service.util.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JWTUtil jwtUtil;
    private UserService userService;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtil = mock(JWTUtil.class);
        userService = mock(UserService.class);
        authController = new AuthController(userRepository, passwordEncoder, jwtUtil, userService);
    }

    @Test
    void signupCreatesUserWithEncodedPasswordAndRole() {
        SignupRequest request = new SignupRequest("Alice", "alice@example.com", "@alice", "password123", null);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByPayTag("@alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userService.createUser(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(7L);
            return user;
        });

        ResponseEntity<?> response = authController.signup(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuthMessageResponse body = (AuthMessageResponse) response.getBody();
        assertThat(body.getUserId()).isEqualTo(7L);
        verify(userRepository).findByPayTag("@alice");
        verify(userService).createUser(any(User.class));
    }

    @Test
    void signupRejectsDuplicateEmail() {
        SignupRequest request = new SignupRequest("Alice", "alice@example.com", "@alice", "password123", null);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(new User()));

        ResponseEntity<?> response = authController.signup(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(userService, never()).createUser(any(User.class));
    }

    @Test
    void signupRejectsDuplicatePayTag() {
        SignupRequest request = new SignupRequest("Alice", "alice@example.com", "@alice", "password123", null);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByPayTag("@alice")).thenReturn(Optional.of(new User()));

        ResponseEntity<?> response = authController.signup(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((AuthMessageResponse) response.getBody()).getMessage()).isEqualTo("PayTag is already taken");
        verify(userService, never()).createUser(any(User.class));
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        User user = new User();
        user.setId(1L);
        user.setEmail("ani@example.com");
        user.setPassword("encoded");
        user.setRole("ROLE_USER");
        when(userRepository.findByEmail("ani@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "ani@example.com", "ROLE_USER")).thenReturn("jwt-token");

        ResponseEntity<?> response = authController.login(new LoginRequest("ani@example.com", "password123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((JwtResponse) response.getBody()).getToken()).isEqualTo("jwt-token");
    }

    @Test
    void loginUsesSameMessageForMissingUserAndBadPassword() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ResponseEntity<?> missingUser = authController.login(new LoginRequest("missing@example.com", "password123"));

        assertThat(missingUser.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(((AuthMessageResponse) missingUser.getBody()).getMessage()).isEqualTo("Invalid credentials");
    }

    @Test
    void loginUsesSameMessageForBadPasswordAndDoesNotIssueToken() {
        User user = new User();
        user.setId(1L);
        user.setEmail("ani@example.com");
        user.setPassword("encoded");
        user.setRole("ROLE_USER");
        when(userRepository.findByEmail("ani@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded")).thenReturn(false);

        ResponseEntity<?> response = authController.login(new LoginRequest("ani@example.com", "wrong-password"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(((AuthMessageResponse) response.getBody()).getMessage()).isEqualTo("Invalid credentials");
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }
}
