package com.paypal.wallet_service.scheduler;

import com.paypal.wallet_service.entity.Wallet;
import com.paypal.wallet_service.entity.WalletHold;
import com.paypal.wallet_service.repository.WalletHoldRepository;
import com.paypal.wallet_service.service.WalletService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HoldExpirySchedulerTest {

    @Test
    void releasesEveryExpiredActiveHold() {
        WalletHold first = hold("HOLD-1");
        WalletHold second = hold("HOLD-2");
        WalletHoldRepository repository = mock(WalletHoldRepository.class);
        WalletService walletService = mock(WalletService.class);
        when(repository.findByStatusAndExpiresAtBefore(eq("ACTIVE"), any(LocalDateTime.class)))
                .thenReturn(List.of(first, second));

        new HoldExpiryScheduler(repository, walletService).expireOldHolds();

        verify(walletService).releaseHold("HOLD-1");
        verify(walletService).releaseHold("HOLD-2");
    }

    @Test
    void continuesSweepWhenOneReleaseFails() {
        WalletHold first = hold("HOLD-1");
        WalletHold second = hold("HOLD-2");
        WalletHoldRepository repository = mock(WalletHoldRepository.class);
        WalletService walletService = mock(WalletService.class);
        when(repository.findByStatusAndExpiresAtBefore(eq("ACTIVE"), any(LocalDateTime.class)))
                .thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("already captured")).when(walletService).releaseHold("HOLD-1");

        new HoldExpiryScheduler(repository, walletService).expireOldHolds();

        verify(walletService).releaseHold("HOLD-1");
        verify(walletService).releaseHold("HOLD-2");
    }

    private WalletHold hold(String reference) {
        Wallet wallet = new Wallet(10L, "INR");
        wallet.setId(1L);
        WalletHold hold = new WalletHold();
        hold.setWallet(wallet);
        hold.setHoldReference(reference);
        hold.setAmount(new BigDecimal("10.00"));
        hold.setStatus("ACTIVE");
        hold.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        return hold;
    }
}
