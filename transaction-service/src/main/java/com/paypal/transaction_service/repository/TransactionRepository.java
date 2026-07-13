package com.paypal.transaction_service.repository;

import com.paypal.transaction_service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {


    @Query("""
            select t from Transaction t
            where t.senderId = :userId
               or (t.receiverId = :userId and t.status = 'SUCCESS')
            """)
    List<Transaction> findVisibleHistoryByUserId(@Param("userId") Long userId);

    List<Transaction> findByStatusIn(List<String> statuses);

    List<Transaction> findByStatusInAndUpdatedAtBefore(List<String> statuses, LocalDateTime updatedBefore);

    Optional<Transaction> findBySenderIdAndIdempotencyKey(Long senderId, String idempotencyKey);
}
