package com.paypal.wallet_service.controller;


import com.paypal.wallet_service.dto.*;
import com.paypal.wallet_service.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        return ResponseEntity.ok(walletService.createWallet(request));
    }

    @PostMapping("/credit")
    public ResponseEntity<WalletResponse> credit(@Valid @RequestBody CreditRequest request) {
        return ResponseEntity.ok(walletService.credit(request));
    }

    @PostMapping("/debit")
    public ResponseEntity<WalletResponse> debit(@Valid @RequestBody DebitRequest request) {
        return ResponseEntity.ok(walletService.debit(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(walletService.getWallet(userId));
    }

    @PostMapping("/{userId}/top-ups")
    public ResponseEntity<WalletResponse> topUp(@PathVariable("userId") Long userId,
                                                @Valid @RequestBody TopUpRequest request) {
        return ResponseEntity.ok(walletService.topUp(userId, request));
    }

    @PostMapping("/hold")
    public ResponseEntity<HoldResponse> placeHold(@Valid @RequestBody HoldRequest request) {
        return ResponseEntity.ok(walletService.placeHold(request));
    }

    @PostMapping("/capture")
    public ResponseEntity<WalletResponse> capture(@Valid @RequestBody CaptureRequest request) {
        return ResponseEntity.ok(walletService.captureHold(request));
    }

    @PostMapping("/release/{holdReference}")
    public ResponseEntity<HoldResponse> release(@PathVariable("holdReference") String holdReference) {
        return ResponseEntity.ok(walletService.releaseHold(holdReference));
    }



}
