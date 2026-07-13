package com.paypal.reward_service.controller;

import com.paypal.reward_service.repository.RewardRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rewards")
public class RewardController {
    private final RewardRepository rewardRepository;

    public RewardController(RewardRepository rewardRepository) {
        this.rewardRepository = rewardRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAllRewards(@RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }

        return ResponseEntity.ok(rewardRepository.findAll());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getRewardsByUserId(@PathVariable("userId") Long userId,
                                                @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
                                                @RequestHeader(value = "X-User-Role", required = false) String role) {
        Long authenticatedUserId = parseHeaderUserId(userIdHeader);
        if (!isAdmin(role) && !userId.equals(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to view these rewards");
        }

        return ResponseEntity.ok(rewardRepository.findByUserId(userId));
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<?> getRewardByTransactionId(@PathVariable("transactionId") Long transactionId,
                                                      @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
                                                      @RequestHeader(value = "X-User-Role", required = false) String role) {
        return rewardRepository.findByTransactionId(transactionId)
                .<ResponseEntity<?>>map(reward -> {
                    Long authenticatedUserId = parseHeaderUserId(userIdHeader);
                    if (!isAdmin(role) && !reward.getUserId().equals(authenticatedUserId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to view this reward");
                    }
                    return ResponseEntity.ok(reward);
                })
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
