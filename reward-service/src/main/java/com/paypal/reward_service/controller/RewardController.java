package com.paypal.reward_service.controller;



import com.paypal.reward_service.entity.Reward;
import com.paypal.reward_service.repository.RewardRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rewards/")
@CrossOrigin(origins = "http://localhost:3000")
public class RewardController {
    private final RewardRepository rewardRepository;

    public RewardController(RewardRepository rewardRepository) {
        this.rewardRepository = rewardRepository;
    }

    // 🔹 Get all rewards
    @GetMapping
    public List<Reward> getAllRewards() {
        return rewardRepository.findAll();
    }

    // 🔹 Get rewards by user ID
    @GetMapping("/user/{userId}")
    public List<Reward> getRewardsByUserId(@PathVariable Long userId) {
        return rewardRepository.findByUserId(userId);
    }

    // 🔹 Get reward by transaction ID
    @GetMapping("/transaction/{transactionId}")
    public Reward getRewardByTransactionId(@PathVariable Long transactionId) {
        return rewardRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Reward not found for transaction ID: " + transactionId));
    }

}
