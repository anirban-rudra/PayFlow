package com.paypal.user_service.controller;

import com.paypal.user_service.dto.AuthMessageResponse;
import com.paypal.user_service.dto.JwtResponse;
import com.paypal.user_service.dto.LoginRequest;
import com.paypal.user_service.dto.SignupRequest;
import com.paypal.user_service.entity.User;
import com.paypal.user_service.repository.UserRepository;
import com.paypal.user_service.service.UserService;
import com.paypal.user_service.util.JWTUtil;
import com.paypal.user_service.util.PayTagUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/auth")
@Tag(name = "User API", description = "Operations related to users")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private final UserService userService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JWTUtil jwtUtil, UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @PostMapping("/signup")
    @Operation(summary = "Sign up a new user", description = "Creates a new user and wallet")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        String normalizedPayTag = PayTagUtil.normalize(request.getPayTag());
        if (!PayTagUtil.isValid(normalizedPayTag)) {
            return ResponseEntity
                    .badRequest()
                    .body(new AuthMessageResponse("PayTag must start with @ and contain 3-30 letters, numbers, dots, underscores, or hyphens"));
        }

        // 1. Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(new AuthMessageResponse("User already exists"));
        }
        if (userRepository.findByPayTag(normalizedPayTag).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(new AuthMessageResponse("PayTag is already taken"));
        }

        // 2. Map request -> User entity
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPayTag(normalizedPayTag);
        user.setRole("ROLE_USER");
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // 3. Use service to create user + wallet
        User savedUser = userService.createUser(user);

        // 4. Return safe response
        return ResponseEntity.ok(new AuthMessageResponse("User registered successfully", savedUser.getId()));
    }






    @PostMapping("/login")
    @Operation(summary = "Login a user", description = "login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(new AuthMessageResponse("Invalid credentials"));
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body(new AuthMessageResponse("Invalid credentials"));
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());

        return ResponseEntity.ok(new JwtResponse(token));
    }

}
