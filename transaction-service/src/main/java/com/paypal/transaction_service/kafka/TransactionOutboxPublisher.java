package com.paypal.transaction_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.transaction_service.entity.Transaction;
import com.paypal.transaction_service.entity.TransactionOutboxEvent;
import com.paypal.transaction_service.repository.TransactionOutboxEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class TransactionOutboxPublisher {

    private final TransactionOutboxEventRepository repository;
    private final KafkaEventProducer kafkaEventProducer;
    private final ObjectMapper objectMapper;

    public TransactionOutboxPublisher(TransactionOutboxEventRepository repository,
                                      KafkaEventProducer kafkaEventProducer,
                                      ObjectMapper objectMapper) {
        this.repository = repository;
        this.kafkaEventProducer = kafkaEventProducer;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher.fixed-delay-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        List<TransactionOutboxEvent> events = repository
                .findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc("PENDING", LocalDateTime.now());

        for (TransactionOutboxEvent event : events) {
            try {
                Transaction transaction = objectMapper.readValue(event.getPayload(), Transaction.class);
                kafkaEventProducer.sendTransactionEventAndWait(event.getEventKey(), transaction);
                event.setStatus("PUBLISHED");
                event.setPublishedAt(LocalDateTime.now());
                event.setLastError(null);
            } catch (Exception ex) {
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(ex.getMessage());
                event.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoffSeconds(event.getAttempts())));
            }
        }
    }

    private long backoffSeconds(int attempts) {
        return Math.min(300, Math.max(5, attempts * 10L));
    }
}
