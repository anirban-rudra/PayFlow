package com.paypal.transaction_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paypal.transaction_service.client.UserClient;
import com.paypal.transaction_service.client.WalletClient;
import com.paypal.transaction_service.dto.CreateTransactionRequest;
import com.paypal.transaction_service.dto.ReconciliationResponse;
import com.paypal.transaction_service.dto.TransactionResponse;
import com.paypal.transaction_service.dto.UserResolutionResponse;
import com.paypal.transaction_service.dto.dto.HoldResponse;
import com.paypal.transaction_service.dto.dto.WalletResponse;
import com.paypal.transaction_service.entity.Transaction;
import com.paypal.transaction_service.entity.TransactionOutboxEvent;
import com.paypal.transaction_service.entity.TransactionStatus;
import com.paypal.transaction_service.repository.TransactionOutboxEventRepository;
import com.paypal.transaction_service.repository.TransactionRepository;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionServiceImplTest {

    @Test
    void createTransactionReturnsExistingTransactionForIdempotentReplay() {
        TransactionRepository repository = mock(TransactionRepository.class);
        TransactionOutboxEventRepository outboxRepository = mock(TransactionOutboxEventRepository.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        UserClient userClient = mock(UserClient.class);
        WalletClient walletClient = mock(WalletClient.class);
        TransactionServiceImpl service = new TransactionServiceImpl(
                repository,
                outboxRepository,
                transactionTemplate,
                userClient,
                walletClient,
                objectMapper(),
                120
        );

        Transaction existing = new Transaction();
        existing.setId(42L);
        existing.setSenderId(10L);
        existing.setReceiverId(20L);
        existing.setAmount(new BigDecimal("500.00"));
        existing.setStatus("SUCCESS");
        existing.setTimestamp(LocalDateTime.parse("2026-07-10T03:00:00"));
        existing.setIdempotencyKey("transfer-1");

        Transaction replayRequest = new Transaction();
        replayRequest.setSenderId(10L);
        replayRequest.setReceiverId(20L);
        replayRequest.setAmount(new BigDecimal("500.00"));

        when(repository.findBySenderIdAndIdempotencyKey(10L, "transfer-1"))
                .thenReturn(Optional.of(existing));

        TransactionResponse response = service.createTransaction(replayRequest, " transfer-1 ");

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).contains("Idempotent replay");
        verify(repository, never()).save(any(Transaction.class));
        verify(walletClient, never()).placeHold(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void createTransactionCompletesWalletFlowAndEnqueuesOutboxEvent() {
        Fixture fixture = fixture();
        Transaction request = transaction(null, 10L, 20L, "50.00", null);
        Transaction pending = transaction(100L, 10L, 20L, "50.00", TransactionStatus.CREATED.name());
        when(fixture.repository.save(any(Transaction.class))).thenReturn(pending, pending);
        when(fixture.walletClient.placeHold(any())).thenReturn(new HoldResponse("HOLD-1", new BigDecimal("50.00"), "ACTIVE"));
        when(fixture.walletClient.getWallet(20L)).thenReturn(new WalletResponse(2L, 20L, "INR", BigDecimal.ZERO, BigDecimal.ZERO));
        when(fixture.transactionTemplate.execute(any())).thenAnswer(invocation -> invocation.<org.springframework.transaction.support.TransactionCallback<Transaction>>getArgument(0).doInTransaction(null));

        TransactionResponse response = fixture.service.createTransaction(request, "payment-1");

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("Transaction completed successfully");
        verify(fixture.walletClient).capture(any());
        verify(fixture.walletClient).credit(any());
        verify(fixture.outboxRepository).save(any(TransactionOutboxEvent.class));
    }

    @Test
    void createTransactionResolvesReceiverPayTagServerSide() {
        Fixture fixture = fixture();
        CreateTransactionRequest request = createRequest("@Receiver", "50.00");
        UserResolutionResponse sender = new UserResolutionResponse();
        sender.setUserId(10L);
        sender.setPayTag("@sender");
        UserResolutionResponse receiver = new UserResolutionResponse();
        receiver.setUserId(20L);
        receiver.setPayTag("@receiver");
        receiver.setDisplayName("Receiver");
        Transaction pending = transaction(100L, 10L, 20L, "50.00", TransactionStatus.CREATED.name());
        pending.setSenderPayTag("@sender");
        pending.setReceiverPayTag("@receiver");

        when(fixture.userClient.resolveUser(10L)).thenReturn(sender);
        when(fixture.userClient.resolvePayTag("@receiver")).thenReturn(receiver);
        when(fixture.repository.save(any(Transaction.class))).thenReturn(pending, pending);
        when(fixture.walletClient.placeHold(any())).thenReturn(new HoldResponse("HOLD-1", new BigDecimal("50.00"), "ACTIVE"));
        when(fixture.walletClient.getWallet(20L)).thenReturn(new WalletResponse(2L, 20L, "INR", BigDecimal.ZERO, BigDecimal.ZERO));
        when(fixture.transactionTemplate.execute(any())).thenAnswer(invocation -> invocation.<org.springframework.transaction.support.TransactionCallback<Transaction>>getArgument(0).doInTransaction(null));

        TransactionResponse response = fixture.service.createTransaction(request, 10L, "payment-1");

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getSenderPayTag()).isEqualTo("@sender");
        assertThat(response.getReceiverPayTag()).isEqualTo("@receiver");
        verify(fixture.userClient).resolveUser(10L);
        verify(fixture.userClient).resolvePayTag("@receiver");
    }

    @Test
    void createTransactionRejectsSelfTransferByPayTag() {
        Fixture fixture = fixture();
        UserResolutionResponse receiver = new UserResolutionResponse();
        receiver.setUserId(10L);
        receiver.setPayTag("@sender");
        when(fixture.userClient.resolvePayTag("@sender")).thenReturn(receiver);

        assertThatThrownBy(() -> fixture.service.createTransaction(createRequest("@sender", "50.00"), 10L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You cannot send money to your own PayTag");

        verify(fixture.walletClient, never()).placeHold(any());
    }

    @Test
    void createTransactionRejectsNullZeroNegativeAndOverPreciseAmounts() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> fixture.service.createTransaction(transaction(null, 10L, 20L, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount is required");
        assertThatThrownBy(() -> fixture.service.createTransaction(transaction(null, 10L, 20L, "0.00", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be positive");
        assertThatThrownBy(() -> fixture.service.createTransaction(transaction(null, 10L, 20L, "1.001", null)))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void createTransactionReleasesHoldWhenReceiverWalletCheckFails() {
        Fixture fixture = fixture();
        Transaction pending = transaction(100L, 10L, 20L, "50.00", TransactionStatus.CREATED.name());
        when(fixture.repository.save(any(Transaction.class))).thenReturn(pending);
        when(fixture.walletClient.placeHold(any())).thenReturn(new HoldResponse("HOLD-1", new BigDecimal("50.00"), "ACTIVE"));
        FeignException receiverFailure = mock(FeignException.class);
        when(receiverFailure.contentUTF8()).thenReturn("WalletNotFoundException");
        when(fixture.walletClient.getWallet(20L)).thenThrow(receiverFailure);

        TransactionResponse response = fixture.service.createTransaction(transaction(null, 10L, 20L, "50.00", null), null);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).isEqualTo("Receiver wallet check failed");
        verify(fixture.walletClient).release("HOLD-1");
        verify(fixture.outboxRepository, never()).save(any());
    }

    @Test
    void createTransactionReturnsFriendlyInsufficientFundsMessageAndDoesNotReleaseWithoutHold() {
        Fixture fixture = fixture();
        Transaction pending = transaction(100L, 10L, 20L, "50.00", TransactionStatus.CREATED.name());
        when(fixture.repository.save(any(Transaction.class))).thenReturn(pending);
        FeignException holdFailure = mock(FeignException.class);
        when(holdFailure.contentUTF8()).thenReturn("InsufficientFundsException: Not enough balance");
        when(fixture.walletClient.placeHold(any())).thenThrow(holdFailure);

        TransactionResponse response = fixture.service.createTransaction(transaction(null, 10L, 20L, "50.00", null), null);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).isEqualTo("Insufficient funds in your wallet");
        verify(fixture.walletClient, never()).release(any());
        verify(fixture.outboxRepository, never()).save(any());
    }

    @Test
    void createTransactionFailsCleanlyWhenHoldResponseIsMissingReference() {
        Fixture fixture = fixture();
        Transaction pending = transaction(100L, 10L, 20L, "50.00", TransactionStatus.CREATED.name());
        when(fixture.repository.save(any(Transaction.class))).thenReturn(pending);
        when(fixture.walletClient.placeHold(any())).thenReturn(new HoldResponse(null, new BigDecimal("50.00"), "ACTIVE"));

        TransactionResponse response = fixture.service.createTransaction(transaction(null, 10L, 20L, "50.00", null), null);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getMessage()).isEqualTo("Failed to place hold on sender's wallet");
        verify(fixture.walletClient, never()).capture(any());
        verify(fixture.outboxRepository, never()).save(any());
    }

    @Test
    void createTransactionRefundsSenderWhenReceiverCreditFailsAfterCapture() {
        Fixture fixture = fixture();
        Transaction pending = transaction(100L, 10L, 20L, "50.00", TransactionStatus.CREATED.name());
        when(fixture.repository.save(any(Transaction.class))).thenReturn(pending);
        when(fixture.walletClient.placeHold(any())).thenReturn(new HoldResponse("HOLD-1", new BigDecimal("50.00"), "ACTIVE"));
        when(fixture.walletClient.getWallet(20L)).thenReturn(new WalletResponse(2L, 20L, "INR", BigDecimal.ZERO, BigDecimal.ZERO));
        FeignException creditFailure = mock(FeignException.class);
        when(creditFailure.contentUTF8()).thenReturn("credit failed");
        doThrow(creditFailure)
                .doReturn(new WalletResponse(1L, 10L, "INR", new BigDecimal("50.00"), new BigDecimal("50.00")))
                .when(fixture.walletClient).credit(any());

        TransactionResponse response = fixture.service.createTransaction(transaction(null, 10L, 20L, "50.00", null), null);

        assertThat(response.getStatus()).isEqualTo("REFUNDED");
        assertThat(response.getMessage()).contains("funds refunded to sender");
        assertThat(response.getFailureReason()).contains("funds refunded to sender");
        assertThat(response.getCompletedAt()).isNotNull();
        verify(fixture.walletClient, never()).release(eq("HOLD-1"));
        verify(fixture.outboxRepository, never()).save(any());
    }

    @Test
    void createTransactionReportsSupportMessageWhenCompensatingRefundFails() {
        Fixture fixture = fixture();
        Transaction pending = transaction(100L, 10L, 20L, "50.00", TransactionStatus.CREATED.name());
        when(fixture.repository.save(any(Transaction.class))).thenReturn(pending);
        when(fixture.walletClient.placeHold(any())).thenReturn(new HoldResponse("HOLD-1", new BigDecimal("50.00"), "ACTIVE"));
        when(fixture.walletClient.getWallet(20L)).thenReturn(new WalletResponse(2L, 20L, "INR", BigDecimal.ZERO, BigDecimal.ZERO));
        FeignException creditFailure = mock(FeignException.class);
        when(creditFailure.contentUTF8()).thenReturn("credit failed");
        doThrow(creditFailure)
                .doThrow(new RuntimeException("refund failed"))
                .when(fixture.walletClient).credit(any());

        TransactionResponse response = fixture.service.createTransaction(transaction(null, 10L, 20L, "50.00", null), null);

        assertThat(response.getStatus()).isEqualTo("MANUAL_REVIEW");
        assertThat(response.getMessage()).contains("refund failed, contact support");
        assertThat(response.getFailureReason()).contains("refund failed, contact support");
        assertThat(response.getCompletedAt()).isNotNull();
        verify(fixture.outboxRepository, never()).save(any());
    }

    @Test
    void queryMethodsDelegateToRepository() {
        Fixture fixture = fixture();
        Transaction transaction = transaction(100L, 10L, 20L, "50.00", "SUCCESS");
        UserResolutionResponse sender = new UserResolutionResponse();
        sender.setUserId(10L);
        sender.setPayTag("@sender");
        when(fixture.repository.findById(100L)).thenReturn(Optional.of(transaction));
        when(fixture.repository.findById(404L)).thenReturn(Optional.empty());
        when(fixture.repository.findVisibleHistoryByUserId(10L)).thenReturn(java.util.List.of(transaction));
        when(fixture.userClient.resolveUser(10L)).thenReturn(sender);
        when(fixture.repository.findByStatusIn(java.util.List.of("CREATED", "HOLD_PLACED", "CAPTURED", "CREDITED", "REFUND_PENDING", "MANUAL_REVIEW")))
                .thenReturn(java.util.List.of(transaction));

        assertThat(fixture.service.getTransactionById(100L)).isSameAs(transaction);
        assertThat(fixture.service.getTransactionById(404L)).isNull();
        assertThat(fixture.service.getTransactionsByUser(10L)).containsExactly(transaction);
        assertThat(transaction.getSenderPayTag()).isEqualTo("@sender");
        assertThat(fixture.service.getTransactionsNeedingReconciliation()).containsExactly(transaction);
    }

    @Test
    void reconcileStaleCreatedTransactionMarksFailedWithoutWalletCall() {
        Fixture fixture = fixture();
        Transaction created = transaction(100L, 10L, 20L, "50.00", TransactionStatus.CREATED.name());
        when(fixture.repository.findById(100L)).thenReturn(Optional.of(created));
        when(fixture.repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReconciliationResponse response = fixture.service.reconcileTransaction(100L);

        assertThat(response.getPreviousStatus()).isEqualTo("CREATED");
        assertThat(response.getCurrentStatus()).isEqualTo("FAILED");
        assertThat(response.getAction()).isEqualTo("MARK_FAILED");
        verify(fixture.walletClient, never()).release(any());
        verify(fixture.walletClient, never()).credit(any());
    }

    @Test
    void reconcileStaleHoldReleasesHoldAndMarksFailed() {
        Fixture fixture = fixture();
        Transaction held = transaction(100L, 10L, 20L, "50.00", TransactionStatus.HOLD_PLACED.name());
        held.setHoldReference("HOLD-1");
        when(fixture.repository.findById(100L)).thenReturn(Optional.of(held));
        when(fixture.repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReconciliationResponse response = fixture.service.reconcileTransaction(100L);

        assertThat(response.getCurrentStatus()).isEqualTo("FAILED");
        assertThat(response.getAction()).isEqualTo("RELEASE_HOLD");
        verify(fixture.walletClient).release("HOLD-1");
    }

    @Test
    void reconcileCapturedTransactionRefundsSender() {
        Fixture fixture = fixture();
        Transaction captured = transaction(100L, 10L, 20L, "50.00", TransactionStatus.CAPTURED.name());
        when(fixture.repository.findById(100L)).thenReturn(Optional.of(captured));
        when(fixture.repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReconciliationResponse response = fixture.service.reconcileTransaction(100L);

        assertThat(response.getCurrentStatus()).isEqualTo("REFUNDED");
        assertThat(response.getAction()).isEqualTo("REFUND_SENDER");
        verify(fixture.walletClient).credit(any());
    }

    @Test
    void reconcileCreditedTransactionMarksSuccessAndEnqueuesOutboxEvent() {
        Fixture fixture = fixture();
        Transaction credited = transaction(100L, 10L, 20L, "50.00", TransactionStatus.CREDITED.name());
        when(fixture.repository.findById(100L)).thenReturn(Optional.of(credited));
        when(fixture.transactionTemplate.execute(any())).thenAnswer(invocation -> invocation.<org.springframework.transaction.support.TransactionCallback<Transaction>>getArgument(0).doInTransaction(null));
        when(fixture.repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReconciliationResponse response = fixture.service.reconcileTransaction(100L);

        assertThat(response.getCurrentStatus()).isEqualTo("SUCCESS");
        assertThat(response.getAction()).isEqualTo("MARK_SUCCESS");
        verify(fixture.outboxRepository).save(any(TransactionOutboxEvent.class));
    }

    private Fixture fixture() {
        TransactionRepository repository = mock(TransactionRepository.class);
        TransactionOutboxEventRepository outboxRepository = mock(TransactionOutboxEventRepository.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        UserClient userClient = mock(UserClient.class);
        WalletClient walletClient = mock(WalletClient.class);
        TransactionServiceImpl service = new TransactionServiceImpl(
                repository,
                outboxRepository,
                transactionTemplate,
                userClient,
                walletClient,
                objectMapper(),
                120
        );
        return new Fixture(repository, outboxRepository, transactionTemplate, userClient, walletClient, service);
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private Transaction transaction(Long id, Long senderId, Long receiverId, String amount, String status) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setSenderId(senderId);
        transaction.setReceiverId(receiverId);
        if (amount != null) {
            transaction.setAmount(new BigDecimal(amount));
        }
        transaction.setStatus(status);
        transaction.setTimestamp(LocalDateTime.parse("2026-07-10T03:00:00"));
        return transaction;
    }

    private CreateTransactionRequest createRequest(String receiverPayTag, String amount) {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setReceiverPayTag(receiverPayTag);
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private record Fixture(
            TransactionRepository repository,
            TransactionOutboxEventRepository outboxRepository,
            TransactionTemplate transactionTemplate,
            UserClient userClient,
            WalletClient walletClient,
            TransactionServiceImpl service
    ) {
    }
}
