package com.paypal.transaction_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.transaction_service.client.UserClient;
import com.paypal.transaction_service.client.WalletClient;
import com.paypal.transaction_service.dto.CreateTransactionRequest;
import com.paypal.transaction_service.dto.ReconciliationResponse;
import com.paypal.transaction_service.dto.TransactionResponse;
import com.paypal.transaction_service.dto.UserResolutionResponse;
import com.paypal.transaction_service.dto.dto.CaptureRequest;
import com.paypal.transaction_service.dto.dto.CreditRequest;
import com.paypal.transaction_service.dto.dto.HoldRequest;
import com.paypal.transaction_service.dto.dto.HoldResponse;
import com.paypal.transaction_service.entity.Transaction;
import com.paypal.transaction_service.entity.TransactionOutboxEvent;
import com.paypal.transaction_service.entity.TransactionStatus;
import com.paypal.transaction_service.repository.TransactionOutboxEventRepository;
import com.paypal.transaction_service.repository.TransactionRepository;
import com.paypal.transaction_service.util.PayTagUtil;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository repository;
    private final TransactionOutboxEventRepository outboxRepository;
    private final TransactionTemplate transactionTemplate;
    private final UserClient userClient;
    private final WalletClient walletClient;
    private final ObjectMapper objectMapper;
    private final long reconciliationStaleAfterSeconds;

    public TransactionServiceImpl(TransactionRepository repository,
                                  TransactionOutboxEventRepository outboxRepository,
                                  TransactionTemplate transactionTemplate,
                                  UserClient userClient,
                                  WalletClient walletClient,
                                  ObjectMapper objectMapper,
                                  @Value("${app.reconciliation.stale-after-seconds:120}") long reconciliationStaleAfterSeconds) {
        this.repository = repository;
        this.outboxRepository = outboxRepository;
        this.transactionTemplate = transactionTemplate;
        this.userClient = userClient;
        this.walletClient = walletClient;
        this.objectMapper = objectMapper;
        this.reconciliationStaleAfterSeconds = reconciliationStaleAfterSeconds;
    }

    @Override
    public TransactionResponse createTransaction(Transaction request) {
        return createTransaction(request, null);
    }

    @Override
    public TransactionResponse createTransaction(Transaction request, String idempotencyKey) {
        Long senderId = request.getSenderId();
        Long receiverId = request.getReceiverId();
        BigDecimal amount = normalizeMoney(request.getAmount());
        request.setAmount(amount);
        String failureReason = null;

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String normalizedKey = idempotencyKey.trim();
            Transaction existing = repository.findBySenderIdAndIdempotencyKey(senderId, normalizedKey).orElse(null);
            if (existing != null) {
                return createResponse(existing, "Idempotent replay: returning existing transaction");
            }
            request.setIdempotencyKey(normalizedKey);
        }

        request.setStatus(TransactionStatus.CREATED);
        request.setTimestamp(LocalDateTime.now());
        Transaction savedTransaction = repository.save(request);
        log.info("Transaction {} saved as CREATED", savedTransaction.getId());

        String holdReference = null;
        boolean captured = false;

        try {
            HoldRequest holdRequest = new HoldRequest();
            holdRequest.setUserId(senderId);
            holdRequest.setCurrency("INR");
            holdRequest.setAmount(amount);
            HoldResponse holdResponse = walletClient.placeHold(holdRequest);

            if (holdResponse == null || holdResponse.getHoldReference() == null) {
                failureReason = "Failed to place hold on sender's wallet";
                throw new IllegalStateException(failureReason);
            }
            holdReference = holdResponse.getHoldReference();
            savedTransaction.setHoldReference(holdReference);
            savedTransaction = transition(savedTransaction, TransactionStatus.HOLD_PLACED, null);
            log.info("Wallet hold placed for transaction {}", savedTransaction.getId());

            try {
                walletClient.getWallet(receiverId);
            } catch (FeignException hx) {
                failureReason = "Receiver wallet check failed";
                log.warn("Receiver wallet check failed for transaction {}: {}", savedTransaction.getId(), hx.contentUTF8());
                tryReleaseHold(holdReference);
                savedTransaction = transition(savedTransaction, TransactionStatus.FAILED, failureReason);
                log.warn("Transaction {} failed during receiver wallet check", savedTransaction.getId());
                return createResponse(savedTransaction, failureReason);
            }

            CaptureRequest captureRequest = new CaptureRequest();
            captureRequest.setHoldReference(holdReference);
            walletClient.capture(captureRequest);
            captured = true;
            savedTransaction = transition(savedTransaction, TransactionStatus.CAPTURED, null);
            log.info("Wallet hold captured for transaction {}", savedTransaction.getId());

            try {
                CreditRequest creditRequest = new CreditRequest();
                creditRequest.setUserId(receiverId);
                creditRequest.setCurrency("INR");
                creditRequest.setAmount(amount);
                walletClient.credit(creditRequest);
                savedTransaction = transition(savedTransaction, TransactionStatus.CREDITED, null);
                log.info("Receiver wallet credited for transaction {}", savedTransaction.getId());
            } catch (FeignException creditEx) {
                failureReason = "Failed to credit receiver's wallet";
                log.warn("Receiver credit failed for transaction {}: {}", savedTransaction.getId(), creditEx.contentUTF8());
                savedTransaction = transition(savedTransaction, TransactionStatus.REFUND_PENDING, failureReason);

                try {
                    CreditRequest refundRequest = new CreditRequest();
                    refundRequest.setUserId(senderId);
                    refundRequest.setCurrency("INR");
                    refundRequest.setAmount(amount);
                    walletClient.credit(refundRequest);
                    log.info("Compensating refund succeeded for transaction {}", savedTransaction.getId());
                    failureReason += " - funds refunded to sender";
                    savedTransaction = transition(savedTransaction, TransactionStatus.REFUNDED, failureReason);
                } catch (Exception ex) {
                    log.error("Compensating refund failed for transaction {}", savedTransaction.getId(), ex);
                    failureReason += " - refund failed, contact support";
                    savedTransaction = transition(savedTransaction, TransactionStatus.MANUAL_REVIEW, failureReason);
                }

                log.warn("Transaction {} failed after receiver credit failure", savedTransaction.getId());
                return createResponse(savedTransaction, failureReason);
            }

            savedTransaction = markSuccessAndEnqueueEvent(savedTransaction);
            log.info("Transaction {} completed successfully", savedTransaction.getId());

        } catch (FeignException e) {
            String errorBody = e.contentUTF8();
            log.warn("Wallet service returned error for transaction {}: {}", savedTransaction.getId(), errorBody);

            if (errorBody.contains("InsufficientFundsException") || errorBody.contains("Not enough balance")) {
                failureReason = "Insufficient funds in your wallet";
            } else if (errorBody.contains("WalletNotFoundException")) {
                failureReason = "Wallet not found for the specified user";
            } else {
                failureReason = "Transaction failed - please try again";
            }

            if (holdReference != null && !captured) {
                tryReleaseHold(holdReference);
            }
            savedTransaction = transition(savedTransaction, TransactionStatus.FAILED, failureReason);
            log.warn("Transaction {} failed after wallet service error", savedTransaction.getId());
            return createResponse(savedTransaction, failureReason);
        }
        catch (Exception e) {
            failureReason = e.getMessage();
            log.error("Transaction {} failed unexpectedly", savedTransaction.getId(), e);
            if (holdReference != null && !captured) {
                tryReleaseHold(holdReference);
            }
            savedTransaction = transition(savedTransaction, TransactionStatus.FAILED, failureReason);
            log.warn("Transaction {} saved as FAILED", savedTransaction.getId());
            return createResponse(savedTransaction, failureReason);
        }

        return createResponse(savedTransaction, "Transaction completed successfully");
    }

    @Override
    public TransactionResponse createTransaction(CreateTransactionRequest request, Long senderId, String idempotencyKey) {
        String receiverPayTag = PayTagUtil.normalize(request.getReceiverPayTag());
        if (!PayTagUtil.isValid(receiverPayTag)) {
            throw new IllegalArgumentException("Invalid receiver PayTag");
        }

        UserResolutionResponse receiver = userClient.resolvePayTag(receiverPayTag);
        if (receiver == null || receiver.getUserId() == null) {
            throw new IllegalArgumentException("Recipient not found");
        }
        if (receiver.getUserId().equals(senderId)) {
            throw new IllegalArgumentException("You cannot send money to your own PayTag");
        }

        Transaction transaction = new Transaction();
        transaction.setSenderId(senderId);
        transaction.setReceiverId(receiver.getUserId());
        transaction.setSenderPayTag(resolveUserPayTag(senderId));
        transaction.setReceiverPayTag(receiver.getPayTag());
        transaction.setAmount(request.getAmount());
        return createTransaction(transaction, idempotencyKey);
    }

    private TransactionResponse createResponse(Transaction transaction, String message) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setPublicReference(transaction.getPublicReference());
        response.setSenderId(transaction.getSenderId());
        response.setReceiverId(transaction.getReceiverId());
        response.setSenderPayTag(transaction.getSenderPayTag());
        response.setReceiverPayTag(transaction.getReceiverPayTag());
        response.setAmount(transaction.getAmount());
        response.setTimestamp(transaction.getTimestamp().toString());
        response.setStatus(transaction.getStatus());
        response.setMessage(message);
        response.setHoldReference(transaction.getHoldReference());
        response.setFailureReason(transaction.getFailureReason());
        response.setCompletedAt(transaction.getCompletedAt() == null ? null : transaction.getCompletedAt().toString());
        return response;
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private Transaction markSuccessAndEnqueueEvent(Transaction transaction) {
        return transactionTemplate.execute(status -> {
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setFailureReason(null);
            transaction.setCompletedAt(LocalDateTime.now());
            Transaction saved = repository.save(transaction);

            TransactionOutboxEvent event = new TransactionOutboxEvent();
            event.setAggregateId(saved.getId());
            event.setEventKey(String.valueOf(saved.getId()));
            event.setEventType("TRANSACTION_SUCCESS");
            event.setTopic("txn-initiated");
            try {
                event.setPayload(objectMapper.writeValueAsString(saved));
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to serialize transaction event", ex);
            }
            outboxRepository.save(event);
            return saved;
        });
    }

    private Transaction transition(Transaction transaction, TransactionStatus status, String failureReason) {
        transaction.setStatus(status);
        transaction.setFailureReason(failureReason);
        if (status == TransactionStatus.SUCCESS || status == TransactionStatus.FAILED
                || status == TransactionStatus.REFUNDED || status == TransactionStatus.MANUAL_REVIEW) {
            transaction.setCompletedAt(LocalDateTime.now());
        }
        return repository.save(transaction);
    }

    private void tryReleaseHold(String holdReference) {
        if (holdReference == null) return;
        try {
            log.info("Attempting wallet hold release");
            walletClient.release(holdReference);
        } catch (Exception ex) {
            log.error("Failed to release wallet hold", ex);
        }
    }

    @Override
    public Transaction getTransactionById(Long id) {
        Transaction transaction = repository.findById(id).orElse(null);
        if (transaction != null) {
            enrichSenderPayTag(transaction);
        }
        return transaction;
    }

    public List<Transaction> getTransactionsByUser(Long userId) {
        return repository.findVisibleHistoryByUserId(userId)
                .stream()
                .peek(this::enrichSenderPayTag)
                .toList();
    }

    private void enrichSenderPayTag(Transaction transaction) {
        if (transaction.getSenderPayTag() == null || transaction.getSenderPayTag().isBlank()) {
            transaction.setSenderPayTag(resolveUserPayTag(transaction.getSenderId()));
        }
    }

    private String resolveUserPayTag(Long userId) {
        try {
            UserResolutionResponse user = userClient.resolveUser(userId);
            if (user != null) {
                return user.getPayTag();
            }
        } catch (Exception ex) {
            log.warn("Could not resolve PayTag for user {}", userId);
        }
        return null;
    }

    @Override
    public List<Transaction> getTransactionsNeedingReconciliation() {
        return repository.findByStatusIn(reconcilableStatuses());
    }

    @Override
    public ReconciliationResponse reconcileTransaction(Long transactionId) {
        Transaction transaction = repository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        return reconcile(transaction);
    }

    @Override
    public List<ReconciliationResponse> reconcileStaleTransactions() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(reconciliationStaleAfterSeconds);
        return repository.findByStatusInAndUpdatedAtBefore(reconcilableStatuses(), cutoff)
                .stream()
                .map(this::reconcile)
                .toList();
    }

    private List<String> reconcilableStatuses() {
        return List.of(
                TransactionStatus.CREATED.name(),
                TransactionStatus.HOLD_PLACED.name(),
                TransactionStatus.CAPTURED.name(),
                TransactionStatus.CREDITED.name(),
                TransactionStatus.REFUND_PENDING.name(),
                TransactionStatus.MANUAL_REVIEW.name()
        );
    }

    private ReconciliationResponse reconcile(Transaction transaction) {
        String previousStatus = transaction.getStatus();
        TransactionStatus status = TransactionStatus.valueOf(previousStatus);

        return switch (status) {
            case CREATED -> {
                Transaction updated = transition(transaction, TransactionStatus.FAILED, "Transaction expired before wallet hold was placed");
                yield response(updated, previousStatus, "MARK_FAILED", "No wallet side effect was recorded");
            }
            case HOLD_PLACED -> reconcilePlacedHold(transaction, previousStatus);
            case CAPTURED, REFUND_PENDING -> reconcileCapturedFunds(transaction, previousStatus);
            case CREDITED -> {
                Transaction updated = markSuccessAndEnqueueEvent(transaction);
                yield response(updated, previousStatus, "MARK_SUCCESS", "Receiver credit had already completed");
            }
            case MANUAL_REVIEW -> response(transaction, previousStatus, "MANUAL_REVIEW", "Manual operator review is required");
            default -> response(transaction, previousStatus, "NOOP", "Transaction is not reconcilable");
        };
    }

    private ReconciliationResponse reconcilePlacedHold(Transaction transaction, String previousStatus) {
        if (transaction.getHoldReference() == null || transaction.getHoldReference().isBlank()) {
            Transaction updated = transition(transaction, TransactionStatus.FAILED, "Hold state had no hold reference");
            return response(updated, previousStatus, "MARK_FAILED", "No hold reference was available to release");
        }

        try {
            walletClient.release(transaction.getHoldReference());
            Transaction updated = transition(transaction, TransactionStatus.FAILED, "Expired wallet hold was released");
            return response(updated, previousStatus, "RELEASE_HOLD", "Released stale wallet hold");
        } catch (Exception ex) {
            Transaction updated = transition(transaction, TransactionStatus.MANUAL_REVIEW, "Failed to release stale hold: " + ex.getMessage());
            return response(updated, previousStatus, "MANUAL_REVIEW", "Could not safely release stale wallet hold");
        }
    }

    private ReconciliationResponse reconcileCapturedFunds(Transaction transaction, String previousStatus) {
        try {
            CreditRequest refundRequest = new CreditRequest();
            refundRequest.setUserId(transaction.getSenderId());
            refundRequest.setCurrency("INR");
            refundRequest.setAmount(transaction.getAmount());
            walletClient.credit(refundRequest);
            Transaction updated = transition(transaction, TransactionStatus.REFUNDED, "Reconciliation refunded captured funds to sender");
            return response(updated, previousStatus, "REFUND_SENDER", "Refunded captured funds to sender");
        } catch (Exception ex) {
            Transaction updated = transition(transaction, TransactionStatus.MANUAL_REVIEW, "Reconciliation refund failed: " + ex.getMessage());
            return response(updated, previousStatus, "MANUAL_REVIEW", "Could not safely refund captured funds");
        }
    }

    private ReconciliationResponse response(Transaction transaction, String previousStatus, String action, String message) {
        return new ReconciliationResponse(
                transaction.getId(),
                previousStatus,
                transaction.getStatus(),
                action,
                message
        );
    }
}
