package com.paypal.reward_service.controller;

import com.paypal.reward_service.entity.Reward;
import com.paypal.reward_service.repository.RewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RewardControllerTest {

    private RewardRepository rewardRepository;
    private RewardController controller;

    @BeforeEach
    void setUp() {
        rewardRepository = mock(RewardRepository.class);
        controller = new RewardController(rewardRepository);
    }

    @Test
    void adminCanListAllRewards() {
        when(rewardRepository.findAll()).thenReturn(List.of(reward(1L, 10L, 100L)));

        ResponseEntity<?> response = controller.getAllRewards("ROLE_ADMIN");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(rewardRepository).findAll();
    }

    @Test
    void nonAdminCannotListAllRewards() {
        ResponseEntity<?> response = controller.getAllRewards("ROLE_USER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(rewardRepository, never()).findAll();
    }

    @Test
    void userCanReadOwnRewards() {
        when(rewardRepository.findByUserId(10L)).thenReturn(List.of(reward(1L, 10L, 100L)));

        ResponseEntity<?> response = controller.getRewardsByUserId(10L, "10", "ROLE_USER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(rewardRepository).findByUserId(10L);
    }

    @Test
    void adminCanReadAnyUsersRewards() {
        when(rewardRepository.findByUserId(10L)).thenReturn(List.of(reward(1L, 10L, 100L)));

        ResponseEntity<?> response = controller.getRewardsByUserId(10L, null, "ROLE_ADMIN");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(rewardRepository).findByUserId(10L);
    }

    @Test
    void userCannotReadAnotherUsersRewards() {
        ResponseEntity<?> response = controller.getRewardsByUserId(10L, "99", "ROLE_USER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(rewardRepository, never()).findByUserId(10L);
    }

    @Test
    void userCanReadRewardByTransactionOnlyWhenOwner() {
        when(rewardRepository.findByTransactionId(100L)).thenReturn(Optional.of(reward(1L, 10L, 100L)));

        ResponseEntity<?> response = controller.getRewardByTransactionId(100L, "10", "ROLE_USER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void userCannotReadRewardByTransactionWhenNotOwnerAndMissingRewardReturnsNotFound() {
        when(rewardRepository.findByTransactionId(100L)).thenReturn(Optional.of(reward(1L, 10L, 100L)));
        when(rewardRepository.findByTransactionId(404L)).thenReturn(Optional.empty());

        ResponseEntity<?> forbidden = controller.getRewardByTransactionId(100L, "99", "ROLE_USER");
        ResponseEntity<?> missing = controller.getRewardByTransactionId(404L, "10", "ROLE_USER");

        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Reward reward(Long id, Long userId, Long transactionId) {
        Reward reward = new Reward();
        reward.setId(id);
        reward.setUserId(userId);
        reward.setTransactionId(transactionId);
        reward.setPoints(new BigDecimal("5.0000"));
        reward.setSentAt(LocalDateTime.now());
        return reward;
    }
}
