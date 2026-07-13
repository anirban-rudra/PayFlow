package com.paypal.reward_service.kafka;

import com.paypal.reward_service.entity.Reward;
import com.paypal.reward_service.entity.Transaction;
import com.paypal.reward_service.repository.RewardRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RewardConsumerTest {

    @Test
    void createsRewardUsingConfiguredRate() {
        RewardRepository repository = mock(RewardRepository.class);
        RewardConsumer consumer = new RewardConsumer(repository, new BigDecimal("0.05"), new BigDecimal("5.00"));
        Transaction transaction = transaction(10L, 100L, "125.25");
        when(repository.existsByTransactionId(10L)).thenReturn(false);

        consumer.consumerTransaction(transaction);

        ArgumentCaptor<Reward> captor = ArgumentCaptor.forClass(Reward.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(100L);
        assertThat(captor.getValue().getTransactionId()).isEqualTo(10L);
        assertThat(captor.getValue().getPoints()).isEqualByComparingTo("6.2625");
    }

    @Test
    void skipsDuplicateRewardEvents() {
        RewardRepository repository = mock(RewardRepository.class);
        RewardConsumer consumer = new RewardConsumer(repository, new BigDecimal("0.05"), new BigDecimal("5.00"));
        Transaction transaction = transaction(10L, 100L, "125.25");
        when(repository.existsByTransactionId(10L)).thenReturn(true);

        consumer.consumerTransaction(transaction);

        verify(repository, never()).save(any());
    }

    @Test
    void skipsTransactionsBelowMinimumRewardAmount() {
        RewardRepository repository = mock(RewardRepository.class);
        RewardConsumer consumer = new RewardConsumer(repository, new BigDecimal("0.05"), new BigDecimal("5.00"));
        Transaction transaction = transaction(10L, 100L, "4.99");

        consumer.consumerTransaction(transaction);

        verify(repository, never()).save(any());
    }

    @Test
    void rewardsTransactionAtExactMinimumAmountAndRoundsHalfUp() {
        RewardRepository repository = mock(RewardRepository.class);
        RewardConsumer consumer = new RewardConsumer(repository, new BigDecimal("0.33333"), new BigDecimal("5.00"));
        Transaction transaction = transaction(11L, 100L, "5.00");

        consumer.consumerTransaction(transaction);

        ArgumentCaptor<Reward> captor = ArgumentCaptor.forClass(Reward.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPoints()).isEqualByComparingTo("1.6667");
    }

    @Test
    void repositoryFailureIsPropagatedForKafkaRetry() {
        RewardRepository repository = mock(RewardRepository.class);
        RewardConsumer consumer = new RewardConsumer(repository, new BigDecimal("0.05"), new BigDecimal("5.00"));
        Transaction transaction = transaction(12L, 100L, "10.00");
        doThrow(new RuntimeException("db unavailable")).when(repository).save(any(Reward.class));

        assertThatThrownBy(() -> consumer.consumerTransaction(transaction))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db unavailable");
    }

    private Transaction transaction(Long id, Long senderId, String amount) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setSenderId(senderId);
        transaction.setReceiverId(200L);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setStatus("SUCCESS");
        return transaction;
    }
}
