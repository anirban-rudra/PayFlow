package com.paypal.wallet_service.service;


import com.paypal.wallet_service.dto.*;
import com.paypal.wallet_service.entity.Transaction;
import com.paypal.wallet_service.entity.Wallet;
import com.paypal.wallet_service.entity.WalletHold;
import com.paypal.wallet_service.entity.WalletHoldStatus;
import com.paypal.wallet_service.entity.WalletTransactionStatus;
import com.paypal.wallet_service.entity.WalletTransactionType;
import com.paypal.wallet_service.exception.InsufficientFundsException;
import com.paypal.wallet_service.exception.NotFoundException;
import com.paypal.wallet_service.repository.TransactionRepository;
import com.paypal.wallet_service.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.paypal.wallet_service.repository.WalletHoldRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;


@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final WalletHoldRepository walletHoldRepository;

    private final TransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository, WalletHoldRepository walletHoldRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.walletHoldRepository = walletHoldRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request) {
        Wallet wallet = new Wallet(request.getUserId(), normalizeCurrency(request.getCurrency()));
        Wallet saved = walletRepository.save(wallet);
        return new WalletResponse(
                saved.getId(), saved.getUserId(), saved.getCurrency(),
                saved.getBalance(), saved.getAvailableBalance()

        );
    }

    @Transactional
    public WalletResponse credit(CreditRequest request) {
        return applyCredit(request.getUserId(), request.getCurrency(), request.getAmount(), WalletTransactionType.CREDIT);
    }

    @Transactional
    public WalletResponse topUp(Long userId, TopUpRequest request) {
        return applyCredit(userId, request.getCurrency(), request.getAmount(), WalletTransactionType.TOP_UP);
    }

    @Transactional
    public WalletResponse debit(DebitRequest request) {
        BigDecimal amount = normalizeMoney(request.getAmount());

        Wallet wallet = walletRepository.findByUserIdAndCurrency(request.getUserId(), normalizeCurrency(request.getCurrency()))
                .orElseThrow(() -> new NotFoundException("Wallet not found for user: "+ request.getUserId()));

        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Not enough balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        Wallet saved = walletRepository.save(wallet);
        recordWalletTransaction(saved.getId(), WalletTransactionType.DEBIT, amount, WalletTransactionStatus.SUCCESS);

        log.info("Wallet {} debited", saved.getId());

        return new WalletResponse(
                saved.getId(), saved.getUserId(), saved.getCurrency(),
                saved.getBalance(), saved.getAvailableBalance()
        );
    }

    public WalletResponse getWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Wallet not found for user: " + userId));

        return new WalletResponse(
                wallet.getId(), wallet.getUserId(), wallet.getCurrency(),
                wallet.getBalance(), wallet.getAvailableBalance()
        );
    }

    @Transactional
    public HoldResponse placeHold(HoldRequest request) {
        BigDecimal amount = normalizeMoney(request.getAmount());

        Wallet wallet = walletRepository.findByUserIdAndCurrency(request.getUserId(), normalizeCurrency(request.getCurrency()))
                .orElseThrow(() -> new NotFoundException("Wallet not found for user: " + request.getUserId()));

        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Not enough balance to hold");
        }

        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));

        WalletHold hold = new WalletHold();
        hold.setWallet(wallet);
        hold.setAmount(amount);
        hold.setHoldReference("HOLD-" + UUID.randomUUID());
        hold.setStatus(WalletHoldStatus.ACTIVE.name());

        walletRepository.save(wallet);
        walletHoldRepository.save(hold);
        recordWalletTransaction(wallet.getId(), WalletTransactionType.HOLD_PLACED, amount, WalletTransactionStatus.SUCCESS);

        return new HoldResponse(hold.getHoldReference(), hold.getAmount(), hold.getStatus());
    }

    @Transactional
    public WalletResponse captureHold(CaptureRequest request) {
        WalletHold hold = walletHoldRepository.findByHoldReference(request.getHoldReference())
                .orElseThrow(() -> new NotFoundException("Hold not found"));

        if (!WalletHoldStatus.ACTIVE.name().equals(hold.getStatus())) {
            throw new IllegalStateException("Hold is not active");
        }

        Wallet wallet = hold.getWallet();
        wallet.setBalance(wallet.getBalance().subtract(hold.getAmount()));

        hold.setStatus(WalletHoldStatus.CAPTURED.name());
        walletRepository.save(wallet);
        walletHoldRepository.save(hold);
        recordWalletTransaction(wallet.getId(), WalletTransactionType.HOLD_CAPTURED, hold.getAmount(), WalletTransactionStatus.SUCCESS);

        return new WalletResponse(wallet.getId(), wallet.getUserId(),
                wallet.getCurrency(), wallet.getBalance(), wallet.getAvailableBalance());
    }

    @Transactional
    public HoldResponse releaseHold(String holdReference) {
        WalletHold hold = walletHoldRepository.findByHoldReference(holdReference)
                .orElseThrow(() -> new NotFoundException("Hold not found"));

        if (!WalletHoldStatus.ACTIVE.name().equals(hold.getStatus())) {
            throw new IllegalStateException("Hold is not active");
        }

        Wallet wallet = hold.getWallet();
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(hold.getAmount()));

        hold.setStatus(WalletHoldStatus.RELEASED.name());
        walletRepository.save(wallet);
        walletHoldRepository.save(hold);
        recordWalletTransaction(wallet.getId(), WalletTransactionType.HOLD_RELEASED, hold.getAmount(), WalletTransactionStatus.SUCCESS);

        return new HoldResponse(hold.getHoldReference(), hold.getAmount(), hold.getStatus());
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "INR";
        }
        return currency.trim().toUpperCase();
    }

    private WalletResponse applyCredit(Long userId, String currency, BigDecimal requestedAmount, WalletTransactionType transactionType) {
        BigDecimal amount = normalizeMoney(requestedAmount);

        Wallet wallet = walletRepository.findByUserIdAndCurrency(userId, normalizeCurrency(currency))
                .orElseThrow(() -> new NotFoundException("Wallet not found for user: " + userId));

        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        Wallet saved = walletRepository.save(wallet);
        recordWalletTransaction(wallet.getId(), transactionType, amount, WalletTransactionStatus.SUCCESS);
        log.info("Wallet {} credited with transaction type {}", saved.getId(), transactionType.name());

        return new WalletResponse(
                saved.getId(), saved.getUserId(), saved.getCurrency(),
                saved.getBalance(), saved.getAvailableBalance()
        );
    }

    private void recordWalletTransaction(Long walletId, WalletTransactionType type, BigDecimal amount, WalletTransactionStatus status) {
        transactionRepository.save(new Transaction(walletId, type.name(), amount, status.name()));
    }


}
