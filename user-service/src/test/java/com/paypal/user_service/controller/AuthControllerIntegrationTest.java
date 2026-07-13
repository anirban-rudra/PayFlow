package com.paypal.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.user_service.client.WalletClient;
import com.paypal.user_service.dto.SignupRequest;
import com.paypal.user_service.dto.WalletResponse;
import com.paypal.user_service.entity.User;
import com.paypal.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private WalletClient walletClient;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        when(walletClient.createWallet(any())).thenReturn(new WalletResponse(1L, 1L, "INR", BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void signupCreatesUserWithEncodedPasswordAndLoginReturnsJwt() throws Exception {
        SignupRequest signup = new SignupRequest("Alice", "alice@example.com", "@alice", "password123", null);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.userId").isNumber());

        User saved = userRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(saved.getPayTag()).isEqualTo("@alice");
        assertThat(saved.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void signupRejectsDuplicateEmail() throws Exception {
        userRepository.save(user("Duplicate", "dup@example.com", "@duplicate", passwordEncoder.encode("password123")));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest("Duplicate", "dup@example.com", "@duplicate2", "password123", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User already exists"));
    }

    @Test
    void signupRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"not-email","password":"short"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginRejectsUnknownUserAndInvalidPayload() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"unknown@example.com","password":"password123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-email","password":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void signupRollsBackUserWhenWalletCreationFails() throws Exception {
        doThrow(new RuntimeException("wallet unavailable")).when(walletClient).createWallet(any());

        assertThatThrownBy(() -> mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignupRequest("Rollback", "rollback@example.com", "@rollback", "password123", null)))))
                .hasCauseInstanceOf(IllegalStateException.class);

        assertThat(userRepository.findByEmail("rollback@example.com")).isEmpty();
    }

    private User user(String name, String email, String payTag, String password) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPayTag(payTag);
        user.setPassword(password);
        user.setRole("ROLE_USER");
        return user;
    }
}
