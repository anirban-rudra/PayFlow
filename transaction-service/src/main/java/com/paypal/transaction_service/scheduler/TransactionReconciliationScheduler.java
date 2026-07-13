package com.paypal.transaction_service.scheduler;

import com.paypal.transaction_service.dto.ReconciliationResponse;
import com.paypal.transaction_service.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.reconciliation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TransactionReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(TransactionReconciliationScheduler.class);

    private final TransactionService transactionService;

    public TransactionReconciliationScheduler(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Scheduled(fixedDelayString = "${app.reconciliation.scan-delay-ms:60000}")
    public void reconcileStaleTransactions() {
        List<ReconciliationResponse> results = transactionService.reconcileStaleTransactions();
        if (!results.isEmpty()) {
            log.info("Reconciled {} stale transaction(s)", results.size());
        }
    }
}
