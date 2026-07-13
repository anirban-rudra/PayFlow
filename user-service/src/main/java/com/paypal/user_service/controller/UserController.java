package com.paypal.user_service.controller;

import com.paypal.user_service.dto.InternalUserResolutionResponse;
import com.paypal.user_service.dto.TransferTargetResponse;
import com.paypal.user_service.dto.UserResponse;
import com.paypal.user_service.repository.UserRepository;
import com.paypal.user_service.service.UserService;
import com.paypal.user_service.util.PayTagUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable("id") Long id,
                                         @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
                                         @RequestHeader(value = "X-User-Role", required = false) String role) {
        Long authenticatedUserId = parseHeaderUserId(userIdHeader);
        if (!isAdmin(role) && !id.equals(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to view this user");
        }

        return userService.getUserById(id)
                .map(UserResponse::from)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers(@RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }

        List<UserResponse> users = userService.getAllUsers().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/resolve-transfer-target")
    public ResponseEntity<?> resolveTransferTarget(@RequestParam("payTag") String payTag) {
        String normalizedPayTag = PayTagUtil.normalize(payTag);
        if (!PayTagUtil.isValid(normalizedPayTag)) {
            return ResponseEntity.badRequest().body("Invalid PayTag");
        }

        return userRepository.findByPayTag(normalizedPayTag)
                .map(TransferTargetResponse::from)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/internal/resolve-pay-tag")
    public ResponseEntity<?> resolvePayTagInternal(@RequestParam("payTag") String payTag) {
        String normalizedPayTag = PayTagUtil.normalize(payTag);
        if (!PayTagUtil.isValid(normalizedPayTag)) {
            return ResponseEntity.badRequest().body("Invalid PayTag");
        }

        return userRepository.findByPayTag(normalizedPayTag)
                .map(InternalUserResolutionResponse::from)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/internal/{id}")
    public ResponseEntity<?> getUserInternal(@PathVariable("id") Long id) {
        return userService.getUserById(id)
                .map(InternalUserResolutionResponse::from)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private boolean isAdmin(String role) {
        return "ROLE_ADMIN".equals(role);
    }

    private Long parseHeaderUserId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
