package com.paypal.transaction_service.service;


import com.paypal.transaction_service.dto.TransactionResponse;
import com.paypal.transaction_service.entity.Transaction;
import org.springframework.stereotype.Service;

import java.util.List;


public interface TransactionService {

    TransactionResponse createTransaction(Transaction transaction);
    public Transaction getTransactionById(Long id);

    public List<Transaction> getTransactionsByUser(Long userId);
}
