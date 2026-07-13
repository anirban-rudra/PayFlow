package com.paypal.transaction_service.service;

import com.paypal.transaction_service.dto.CreateTransactionRequest;
import com.paypal.transaction_service.dto.ReconciliationResponse;
import com.paypal.transaction_service.dto.TransactionResponse;
import com.paypal.transaction_service.entity.Transaction;

import java.util.List;

public interface TransactionService {

    TransactionResponse createTransaction(Transaction transaction);

    TransactionResponse createTransaction(Transaction transaction, String idempotencyKey);

    TransactionResponse createTransaction(CreateTransactionRequest request, Long senderId, String idempotencyKey);

    Transaction getTransactionById(Long id);

    List<Transaction> getTransactionsByUser(Long userId);

    List<Transaction> getTransactionsNeedingReconciliation();

    ReconciliationResponse reconcileTransaction(Long transactionId);

    List<ReconciliationResponse> reconcileStaleTransactions();
}
