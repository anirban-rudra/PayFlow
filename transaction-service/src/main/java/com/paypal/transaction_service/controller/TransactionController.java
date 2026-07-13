package com.paypal.transaction_service.controller;


import com.paypal.transaction_service.dto.CreateTransactionRequest;
import com.paypal.transaction_service.dto.TransactionResponse;
import com.paypal.transaction_service.entity.Transaction;
import com.paypal.transaction_service.entity.TransactionStatus;
import com.paypal.transaction_service.service.TransactionService;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/transactions/")
public class TransactionController {
    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }
    @PostMapping("/create")
    public ResponseEntity<?> create(@Valid @RequestBody CreateTransactionRequest transaction,
                                    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                    HttpServletRequest request) {

        // Read userId from gateway header
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Missing X-User-Id header from gateway");
        }

        Long tokenUserId = parseHeaderUserId(userIdHeader);
        if (tokenUserId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Invalid X-User-Id header from gateway");
        }
        try {
            TransactionResponse created = service.createTransaction(transaction, tokenUserId, idempotencyKey);
            return responseForCreatedTransaction(created);
        } catch (FeignException.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Recipient not found");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }



    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable("id") Long id) {
        Transaction transaction = service.getTransactionById(id);
        if (transaction == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Transaction with id " + id + " not found");
        }
        return ResponseEntity.ok(transaction);
    }


    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getTransactionsByUser(
            @PathVariable("userId") Long userId,
            HttpServletRequest request) {

        // Read JWT userId forwarded by gateway
        String tokenUserIdHeader = request.getHeader("X-User-Id");
        if (tokenUserIdHeader == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Missing X-User-Id header from gateway");
        }

        Long tokenUserId = parseHeaderUserId(tokenUserIdHeader);
        if (tokenUserId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Invalid X-User-Id header from gateway");
        }

        // Ensure user can only fetch their own transactions
        if (!userId.equals(tokenUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You are not authorized to view these transactions.");
        }

        List<Transaction> transactions = service.getTransactionsByUser(userId);

        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/reconciliation")
    public ResponseEntity<?> getTransactionsNeedingReconciliation(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Admin access required");
        }

        return ResponseEntity.ok(service.getTransactionsNeedingReconciliation());
    }

    @PostMapping("/reconciliation/{id}")
    public ResponseEntity<?> reconcileTransaction(@PathVariable("id") Long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Admin access required");
        }

        try {
            return ResponseEntity.ok(service.reconcileTransaction(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/reconciliation/stale")
    public ResponseEntity<?> reconcileStaleTransactions(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Admin access required");
        }

        return ResponseEntity.ok(service.reconcileStaleTransactions());
    }

    private boolean isAdmin(HttpServletRequest request) {
        return "ROLE_ADMIN".equals(request.getHeader("X-User-Role"));
    }

    private Long parseHeaderUserId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ResponseEntity<?> responseForCreatedTransaction(TransactionResponse transaction) {
        if (TransactionStatus.SUCCESS.name().equals(transaction.getStatus())) {
            return ResponseEntity.ok(transaction);
        }

        if (TransactionStatus.FAILED.name().equals(transaction.getStatus())) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(transaction);
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(transaction);
    }

}
