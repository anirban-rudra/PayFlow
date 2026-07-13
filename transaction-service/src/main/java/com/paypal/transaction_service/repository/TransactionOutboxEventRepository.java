package com.paypal.transaction_service.repository;

import com.paypal.transaction_service.entity.TransactionOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionOutboxEventRepository extends JpaRepository<TransactionOutboxEvent, Long> {
    List<TransactionOutboxEvent> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            String status,
            LocalDateTime now
    );
}
