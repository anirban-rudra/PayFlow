package com.paypal.global;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paypal.notification_service.entity.Notification;
import com.paypal.notification_service.kafka.NotificationConsumer;
import com.paypal.notification_service.repository.NotificationRepository;
import com.paypal.reward_service.entity.Reward;
import com.paypal.reward_service.kafka.RewardConsumer;
import com.paypal.reward_service.repository.RewardRepository;
import com.paypal.transaction_service.client.UserClient;
import com.paypal.transaction_service.client.WalletClient;
import com.paypal.transaction_service.dto.TransactionResponse;
import com.paypal.transaction_service.dto.dto.HoldResponse;
import com.paypal.transaction_service.dto.dto.WalletResponse;
import com.paypal.transaction_service.entity.Transaction;
import com.paypal.transaction_service.entity.TransactionOutboxEvent;
import com.paypal.transaction_service.entity.TransactionStatus;
import com.paypal.transaction_service.repository.TransactionOutboxEventRepository;
import com.paypal.transaction_service.repository.TransactionRepository;
import com.paypal.transaction_service.service.TransactionServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlobalPaymentFlowContractTest {

    @Test
    @SuppressWarnings("unchecked")
    void successfulPaymentOutboxPayloadCanDriveRewardAndNotificationConsumers() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        TransactionOutboxEventRepository outboxRepository = mock(TransactionOutboxEventRepository.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        UserClient userClient = mock(UserClient.class);
        WalletClient walletClient = mock(WalletClient.class);
        TransactionServiceImpl transactionService = new TransactionServiceImpl(
                transactionRepository,
                outboxRepository,
                transactionTemplate,
                userClient,
                walletClient,
                objectMapper,
                120
        );

        Transaction saved = new Transaction();
        saved.setId(900L);
        saved.setSenderId(10L);
        saved.setReceiverId(20L);
        saved.setAmount(new BigDecimal("100.00"));
        saved.setTimestamp(LocalDateTime.parse("2026-07-10T03:00:00"));
        saved.setStatus(TransactionStatus.CREATED.name());

        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved, saved);
        when(walletClient.placeHold(any())).thenReturn(new HoldResponse("HOLD-900", new BigDecimal("100.00"), "ACTIVE"));
        when(walletClient.getWallet(20L)).thenReturn(new WalletResponse(2L, 20L, "INR", BigDecimal.ZERO, BigDecimal.ZERO));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> invocation.<org.springframework.transaction.support.TransactionCallback<Transaction>>getArgument(0).doInTransaction(null));

        Transaction request = new Transaction();
        request.setSenderId(10L);
        request.setReceiverId(20L);
        request.setAmount(new BigDecimal("100.00"));

        TransactionResponse response = transactionService.createTransaction(request, "global-flow-1");

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        ArgumentCaptor<TransactionOutboxEvent> outboxCaptor = ArgumentCaptor.forClass(TransactionOutboxEvent.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getTopic()).isEqualTo("txn-initiated");

        com.paypal.reward_service.entity.Transaction rewardEvent =
                objectMapper.readValue(outboxCaptor.getValue().getPayload(), com.paypal.reward_service.entity.Transaction.class);
        RewardRepository rewardRepository = mock(RewardRepository.class);
        when(rewardRepository.existsByTransactionId(900L)).thenReturn(false);
        new RewardConsumer(rewardRepository, new BigDecimal("0.05"), new BigDecimal("5.00")).consumerTransaction(rewardEvent);

        ArgumentCaptor<Reward> rewardCaptor = ArgumentCaptor.forClass(Reward.class);
        verify(rewardRepository).save(rewardCaptor.capture());
        assertThat(rewardCaptor.getValue().getUserId()).isEqualTo(10L);
        assertThat(rewardCaptor.getValue().getPoints()).isEqualByComparingTo("5.0000");

        com.paypal.notification_service.entity.Transaction notificationEvent =
                objectMapper.readValue(outboxCaptor.getValue().getPayload(), com.paypal.notification_service.entity.Transaction.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        new NotificationConsumer(notificationRepository).consumeTransaction(notificationEvent);

        ArgumentCaptor<List<Notification>> notificationCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue()).extracting(Notification::getUserId).containsExactly(10L, 20L);
    }
}
