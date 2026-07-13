package com.paypal.wallet_service;

import com.paypal.wallet_service.dto.CaptureRequest;
import com.paypal.wallet_service.dto.CreateWalletRequest;
import com.paypal.wallet_service.dto.CreditRequest;
import com.paypal.wallet_service.dto.HoldRequest;
import com.paypal.wallet_service.dto.HoldResponse;
import com.paypal.wallet_service.dto.WalletResponse;
import com.paypal.wallet_service.repository.WalletRepository;
import com.paypal.wallet_service.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WalletServiceIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();
    }

    @Test
    void creditHoldCaptureAndReleaseUseConsistentBalances() {
        CreateWalletRequest create = new CreateWalletRequest();
        create.setUserId(100L);
        create.setCurrency("inr");
        WalletResponse wallet = walletService.createWallet(create);
        assertThat(wallet.getCurrency()).isEqualTo("INR");
        assertThat(wallet.getBalance()).isEqualByComparingTo("0.00");
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("0.00");

        WalletResponse credited = walletService.credit(creditRequest(100L, "INR", "100.00"));
        assertThat(credited.getBalance()).isEqualByComparingTo("100.00");
        assertThat(credited.getAvailableBalance()).isEqualByComparingTo("100.00");

        HoldResponse hold = walletService.placeHold(holdRequest(100L, "INR", "30.00"));
        WalletResponse afterHold = walletService.getWallet(100L);
        assertThat(afterHold.getBalance()).isEqualByComparingTo("100.00");
        assertThat(afterHold.getAvailableBalance()).isEqualByComparingTo("70.00");

        CaptureRequest captureRequest = new CaptureRequest();
        captureRequest.setHoldReference(hold.getHoldReference());
        WalletResponse afterCapture = walletService.captureHold(captureRequest);
        assertThat(afterCapture.getBalance()).isEqualByComparingTo("70.00");
        assertThat(afterCapture.getAvailableBalance()).isEqualByComparingTo("70.00");

        HoldResponse secondHold = walletService.placeHold(holdRequest(100L, "INR", "20.00"));
        HoldResponse released = walletService.releaseHold(secondHold.getHoldReference());
        assertThat(released.getStatus()).isEqualTo("RELEASED");
        WalletResponse afterRelease = walletService.getWallet(100L);
        assertThat(afterRelease.getBalance()).isEqualByComparingTo("70.00");
        assertThat(afterRelease.getAvailableBalance()).isEqualByComparingTo("70.00");
    }

    private CreditRequest creditRequest(Long userId, String currency, String amount) {
        CreditRequest request = new CreditRequest();
        request.setUserId(userId);
        request.setCurrency(currency);
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private HoldRequest holdRequest(Long userId, String currency, String amount) {
        HoldRequest request = new HoldRequest();
        request.setUserId(userId);
        request.setCurrency(currency);
        request.setAmount(new BigDecimal(amount));
        return request;
    }
}
