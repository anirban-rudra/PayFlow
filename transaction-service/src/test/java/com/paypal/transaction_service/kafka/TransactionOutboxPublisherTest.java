package com.paypal.transaction_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paypal.transaction_service.entity.Transaction;
import com.paypal.transaction_service.entity.TransactionOutboxEvent;
import com.paypal.transaction_service.repository.TransactionOutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionOutboxPublisherTest {

    private TransactionOutboxEventRepository repository;
    private KafkaEventProducer kafkaEventProducer;
    private ObjectMapper objectMapper;
    private TransactionOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        repository = mock(TransactionOutboxEventRepository.class);
        kafkaEventProducer = mock(KafkaEventProducer.class);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        publisher = new TransactionOutboxPublisher(repository, kafkaEventProducer, objectMapper);
    }

    @Test
    void publishesPendingEventsAndMarksThemPublished() throws Exception {
        TransactionOutboxEvent event = outboxEvent(transaction(100L));
        when(repository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(eq("PENDING"), any(LocalDateTime.class)))
                .thenReturn(List.of(event));

        publisher.publishPendingEvents();

        verify(kafkaEventProducer).sendTransactionEventAndWait(eq("100"), any(Transaction.class));
        assertThat(event.getStatus()).isEqualTo("PUBLISHED");
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getLastError()).isNull();
    }

    @Test
    void failedPublishIncrementsAttemptsAndSchedulesRetry() throws Exception {
        TransactionOutboxEvent event = outboxEvent(transaction(100L));
        event.setAttempts(1);
        LocalDateTime before = LocalDateTime.now();
        when(repository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(eq("PENDING"), any(LocalDateTime.class)))
                .thenReturn(List.of(event));
        doThrow(new RuntimeException("kafka down"))
                .when(kafkaEventProducer).sendTransactionEventAndWait(eq("100"), any(Transaction.class));

        publisher.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo("PENDING");
        assertThat(event.getAttempts()).isEqualTo(2);
        assertThat(event.getLastError()).isEqualTo("kafka down");
        assertThat(event.getNextAttemptAt()).isAfter(before.plusSeconds(15));
        assertThat(event.getPublishedAt()).isNull();
    }

    private TransactionOutboxEvent outboxEvent(Transaction transaction) throws Exception {
        TransactionOutboxEvent event = new TransactionOutboxEvent();
        event.setAggregateId(transaction.getId());
        event.setEventKey(transaction.getId().toString());
        event.setEventType("TRANSACTION_COMPLETED");
        event.setTopic("txn-initiated");
        event.setPayload(objectMapper.writeValueAsString(transaction));
        event.setStatus("PENDING");
        event.setCreatedAt(LocalDateTime.now().minusSeconds(10));
        event.setNextAttemptAt(LocalDateTime.now().minusSeconds(1));
        return event;
    }

    private Transaction transaction(Long id) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setSenderId(10L);
        transaction.setReceiverId(20L);
        transaction.setAmount(new BigDecimal("50.00"));
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setStatus("COMPLETED");
        return transaction;
    }
}
