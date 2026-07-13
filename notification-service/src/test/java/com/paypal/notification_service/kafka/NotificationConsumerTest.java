package com.paypal.notification_service.kafka;

import com.paypal.notification_service.entity.Notification;
import com.paypal.notification_service.entity.Transaction;
import com.paypal.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationConsumerTest {

    @Test
    @SuppressWarnings("unchecked")
    void createsSenderAndReceiverNotificationsForSuccessfulTransaction() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationConsumer consumer = new NotificationConsumer(repository);
        Transaction transaction = new Transaction();
        transaction.setId(77L);
        transaction.setSenderId(10L);
        transaction.setReceiverId(20L);
        transaction.setReceiverPayTag("@receiver");
        transaction.setAmount(new BigDecimal("42.50"));

        consumer.consumeTransaction(transaction);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).extracting(Notification::getUserId).containsExactly(10L, 20L);
        assertThat(captor.getValue().get(0).getMessage()).contains("sent to @receiver");
        assertThat(captor.getValue().get(1).getMessage()).contains("received from a PayFlow user");
        assertThat(captor.getValue()).extracting(Notification::getMessage).noneMatch(message -> message.contains("user 10") || message.contains("user 20"));
        assertThat(captor.getValue().get(0).getSentAt()).isEqualTo(captor.getValue().get(1).getSentAt());
    }

    @Test
    void repositoryFailureIsPropagatedForKafkaRetry() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationConsumer consumer = new NotificationConsumer(repository);
        Transaction transaction = new Transaction();
        transaction.setId(77L);
        transaction.setSenderId(10L);
        transaction.setReceiverId(20L);
        transaction.setAmount(new BigDecimal("42.50"));
        doThrow(new RuntimeException("db unavailable")).when(repository).saveAll(anyList());

        assertThatThrownBy(() -> consumer.consumeTransaction(transaction))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db unavailable");
    }
}
