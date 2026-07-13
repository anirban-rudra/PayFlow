package com.paypal.wallet_service.service;

import com.paypal.wallet_service.dto.CaptureRequest;
import com.paypal.wallet_service.dto.CreateWalletRequest;
import com.paypal.wallet_service.dto.CreditRequest;
import com.paypal.wallet_service.dto.DebitRequest;
import com.paypal.wallet_service.dto.HoldRequest;
import com.paypal.wallet_service.dto.HoldResponse;
import com.paypal.wallet_service.dto.TopUpRequest;
import com.paypal.wallet_service.dto.WalletResponse;
import com.paypal.wallet_service.entity.Transaction;
import com.paypal.wallet_service.entity.Wallet;
import com.paypal.wallet_service.entity.WalletHold;
import com.paypal.wallet_service.exception.InsufficientFundsException;
import com.paypal.wallet_service.exception.NotFoundException;
import com.paypal.wallet_service.repository.TransactionRepository;
import com.paypal.wallet_service.repository.WalletHoldRepository;
import com.paypal.wallet_service.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletServiceTest {

    private WalletRepository walletRepository;
    private WalletHoldRepository walletHoldRepository;
    private TransactionRepository transactionRepository;
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletRepository = mock(WalletRepository.class);
        walletHoldRepository = mock(WalletHoldRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        walletService = new WalletService(walletRepository, walletHoldRepository, transactionRepository);
    }

    @Test
    void createWalletDefaultsBlankCurrencyToInrAndUppercasesConfiguredCurrency() {
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet wallet = invocation.getArgument(0);
            wallet.setId(9L);
            return wallet;
        });

        WalletResponse defaultCurrency = walletService.createWallet(createWalletRequest(10L, " "));
        WalletResponse normalizedCurrency = walletService.createWallet(createWalletRequest(11L, "usd"));

        assertThat(defaultCurrency.getCurrency()).isEqualTo("INR");
        assertThat(normalizedCurrency.getCurrency()).isEqualTo("USD");
    }

    @Test
    void creditAddsToBalanceAndAvailableBalance() {
        Wallet wallet = wallet(1L, 10L, "INR", "100.00", "70.00");
        when(walletRepository.findByUserIdAndCurrency(10L, "INR")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreditRequest request = creditRequest(10L, "inr", "25.50");

        WalletResponse response = walletService.credit(request);

        assertThat(response.getBalance()).isEqualByComparingTo("125.50");
        assertThat(response.getAvailableBalance()).isEqualByComparingTo("95.50");
        verify(transactionRepository).save(any());
    }

    @Test
    void topUpAddsToOwnerBalanceAndRecordsTopUpTransaction() {
        Wallet wallet = wallet(1L, 10L, "INR", "100.00", "70.00");
        when(walletRepository.findByUserIdAndCurrency(10L, "INR")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse response = walletService.topUp(10L, topUpRequest("inr", "45.25"));

        assertThat(response.getBalance()).isEqualByComparingTo("145.25");
        assertThat(response.getAvailableBalance()).isEqualByComparingTo("115.25");

        ArgumentCaptor<Transaction> captor = forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("TOP_UP");
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("45.25");
    }

    @Test
    void creditRejectsNullZeroAndNegativeAmountsBeforePersistence() {
        assertThatThrownBy(() -> walletService.credit(creditRequest(10L, "INR", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount is required");
        assertThatThrownBy(() -> walletService.credit(creditRequest(10L, "INR", "0.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be positive");
        assertThatThrownBy(() -> walletService.credit(creditRequest(10L, "INR", "-1.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be positive");

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void creditThrowsWhenWalletDoesNotExist() {
        when(walletRepository.findByUserIdAndCurrency(10L, "INR")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.credit(creditRequest(10L, "INR", "10.00")))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Wallet not found");
    }

    @Test
    void debitSubtractsOnlyWhenAvailableBalanceIsEnough() {
        Wallet wallet = wallet(1L, 10L, "INR", "100.00", "70.00");
        when(walletRepository.findByUserIdAndCurrency(10L, "INR")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse response = walletService.debit(debitRequest(10L, "INR", "50.00"));

        assertThat(response.getBalance()).isEqualByComparingTo("50.00");
        assertThat(response.getAvailableBalance()).isEqualByComparingTo("20.00");
        ArgumentCaptor<Transaction> captor = forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("DEBIT");
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void debitAllowsExactAvailableBalance() {
        Wallet wallet = wallet(1L, 10L, "INR", "100.00", "70.00");
        when(walletRepository.findByUserIdAndCurrency(10L, "INR")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponse response = walletService.debit(debitRequest(10L, "INR", "70.00"));

        assertThat(response.getBalance()).isEqualByComparingTo("30.00");
        assertThat(response.getAvailableBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void debitRejectsInsufficientAvailableBalance() {
        Wallet wallet = wallet(1L, 10L, "INR", "100.00", "20.00");
        when(walletRepository.findByUserIdAndCurrency(10L, "INR")).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.debit(debitRequest(10L, "INR", "20.01")))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Not enough balance");
    }

    @Test
    void placeHoldReducesAvailableBalanceAndCreatesActiveHold() {
        Wallet wallet = wallet(1L, 10L, "INR", "100.00", "80.00");
        when(walletRepository.findByUserIdAndCurrency(10L, "INR")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletHoldRepository.save(any(WalletHold.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HoldResponse response = walletService.placeHold(holdRequest(10L, "INR", "30.00"));

        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("50.00");
        assertThat(response.getAmount()).isEqualByComparingTo("30.00");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getHoldReference()).startsWith("HOLD-");
        ArgumentCaptor<Transaction> captor = forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("HOLD_PLACED");
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void placeHoldRejectsInsufficientAvailableBalanceAndMissingWallet() {
        Wallet wallet = wallet(1L, 10L, "INR", "100.00", "20.00");
        when(walletRepository.findByUserIdAndCurrency(10L, "INR")).thenReturn(Optional.of(wallet));
        when(walletRepository.findByUserIdAndCurrency(99L, "INR")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.placeHold(holdRequest(10L, "INR", "20.01")))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Not enough balance to hold");
        assertThatThrownBy(() -> walletService.placeHold(holdRequest(99L, "INR", "1.00")))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Wallet not found");
    }

    @Test
    void captureHoldSubtractsBalanceAndMarksHoldCaptured() {
        Wallet wallet = wallet(1L, 10L, "INR", "100.00", "40.00");
        WalletHold hold = hold(wallet, "HOLD-1", "30.00", "ACTIVE");
        when(walletHoldRepository.findByHoldReference("HOLD-1")).thenReturn(Optional.of(hold));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletHoldRepository.save(any(WalletHold.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CaptureRequest request = new CaptureRequest();
        request.setHoldReference("HOLD-1");

        WalletResponse response = walletService.captureHold(request);

        assertThat(response.getBalance()).isEqualByComparingTo("70.00");
        assertThat(response.getAvailableBalance()).isEqualByComparingTo("40.00");
        assertThat(hold.getStatus()).isEqualTo("CAPTURED");
        ArgumentCaptor<Transaction> captor = forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("HOLD_CAPTURED");
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void captureHoldRejectsMissingOrInactiveHold() {
        Wallet wallet = wallet(1L, 10L, "INR", "100.00", "40.00");
        WalletHold released = hold(wallet, "HOLD-RELEASED", "30.00", "RELEASED");
        when(walletHoldRepository.findByHoldReference("HOLD-MISSING")).thenReturn(Optional.empty());
        when(walletHoldRepository.findByHoldReference("HOLD-RELEASED")).thenReturn(Optional.of(released));

        CaptureRequest missing = captureRequest("HOLD-MISSING");
        CaptureRequest inactive = captureRequest("HOLD-RELEASED");

        assertThatThrownBy(() -> walletService.captureHold(missing))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Hold not found");
        assertThatThrownBy(() -> walletService.captureHold(inactive))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Hold is not active");
    }

    @Test
    void releaseHoldRestoresAvailableBalanceAndMarksHoldReleased() {
        Wallet wallet = wallet(1L, 10L, "INR", "100.00", "40.00");
        WalletHold hold = hold(wallet, "HOLD-1", "30.00", "ACTIVE");
        when(walletHoldRepository.findByHoldReference("HOLD-1")).thenReturn(Optional.of(hold));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletHoldRepository.save(any(WalletHold.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HoldResponse response = walletService.releaseHold("HOLD-1");

        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("70.00");
        assertThat(response.getStatus()).isEqualTo("RELEASED");
        ArgumentCaptor<Transaction> captor = forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("HOLD_RELEASED");
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void releaseHoldRejectsMissingOrInactiveHold() {
        Wallet wallet = wallet(1L, 10L, "INR", "100.00", "40.00");
        WalletHold captured = hold(wallet, "HOLD-CAPTURED", "30.00", "CAPTURED");
        when(walletHoldRepository.findByHoldReference("HOLD-MISSING")).thenReturn(Optional.empty());
        when(walletHoldRepository.findByHoldReference("HOLD-CAPTURED")).thenReturn(Optional.of(captured));

        assertThatThrownBy(() -> walletService.releaseHold("HOLD-MISSING"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Hold not found");
        assertThatThrownBy(() -> walletService.releaseHold("HOLD-CAPTURED"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Hold is not active");
    }

    @Test
    void rejectsAmountsWithMoreThanTwoFractionDigits() {
        Wallet wallet = wallet(1L, 10L, "INR", "100.00", "100.00");
        when(walletRepository.findByUserIdAndCurrency(10L, "INR")).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.credit(creditRequest(10L, "INR", "1.001")))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void getWalletThrowsWhenWalletDoesNotExist() {
        when(walletRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getWallet(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Wallet not found");
    }

    private Wallet wallet(Long id, Long userId, String currency, String balance, String availableBalance) {
        Wallet wallet = new Wallet(userId, currency);
        wallet.setId(id);
        wallet.setBalance(new BigDecimal(balance));
        wallet.setAvailableBalance(new BigDecimal(availableBalance));
        return wallet;
    }

    private CreateWalletRequest createWalletRequest(Long userId, String currency) {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(userId);
        request.setCurrency(currency);
        return request;
    }

    private WalletHold hold(Wallet wallet, String reference, String amount, String status) {
        WalletHold hold = new WalletHold();
        hold.setWallet(wallet);
        hold.setHoldReference(reference);
        hold.setAmount(new BigDecimal(amount));
        hold.setStatus(status);
        return hold;
    }

    private CreditRequest creditRequest(Long userId, String currency, String amount) {
        CreditRequest request = new CreditRequest();
        request.setUserId(userId);
        request.setCurrency(currency);
        if (amount != null) {
            request.setAmount(new BigDecimal(amount));
        }
        return request;
    }

    private DebitRequest debitRequest(Long userId, String currency, String amount) {
        DebitRequest request = new DebitRequest();
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

    private TopUpRequest topUpRequest(String currency, String amount) {
        TopUpRequest request = new TopUpRequest();
        request.setCurrency(currency);
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private CaptureRequest captureRequest(String holdReference) {
        CaptureRequest request = new CaptureRequest();
        request.setHoldReference(holdReference);
        return request;
    }
}
