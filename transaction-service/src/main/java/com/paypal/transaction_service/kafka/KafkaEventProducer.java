package com.paypal.transaction_service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paypal.transaction_service.entity.Transaction;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class KafkaEventProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventProducer.class);
    private static final String TOPIC = "txn-initiated";

    private final KafkaTemplate<String, Transaction> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventProducer(KafkaTemplate<String, Transaction> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        // Register module to handle Java 8 date/time serialization
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    public void sendTransactionEvent(String key, Transaction transaction) {
        log.info("Sending transaction {} to Kafka topic {}", transaction.getId(), TOPIC);

        CompletableFuture<SendResult<String, Transaction>> future = kafkaTemplate.send(TOPIC, key, transaction);

        future.thenAccept(result -> {
            RecordMetadata metadata = result.getRecordMetadata();
            log.info("Kafka message sent: topic={}, partition={}, offset={}",
                    metadata.topic(), metadata.partition(), metadata.offset());
        }).exceptionally(ex -> {
            log.error("Failed to send Kafka message", ex);
            return null;
        });
    }

    public void sendTransactionEventAndWait(String key, Transaction transaction) throws Exception {
        kafkaTemplate.send(TOPIC, key, transaction).get(10, TimeUnit.SECONDS);
    }
}
