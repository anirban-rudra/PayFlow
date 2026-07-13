package com.paypal.transaction_service.controller;

import com.paypal.transaction_service.dto.CreateTransactionRequest;
import com.paypal.transaction_service.dto.ReconciliationResponse;
import com.paypal.transaction_service.dto.TransactionResponse;
import com.paypal.transaction_service.entity.Transaction;
import com.paypal.transaction_service.entity.TransactionStatus;
import com.paypal.transaction_service.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionControllerTest {

    private TransactionService service;
    private TransactionController controller;

    @BeforeEach
    void setUp() {
        service = mock(TransactionService.class);
        controller = new TransactionController(service);
    }

    @Test
    void createRejectsMissingGatewayUserHeader() {
        ResponseEntity<?> response = controller.create(createRequest(), "idem-1", new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(service, never()).createTransaction(org.mockito.ArgumentMatchers.any(CreateTransactionRequest.class), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createRejectsMalformedGatewayUserHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "not-a-number");

        ResponseEntity<?> response = controller.create(createRequest(), "idem-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(service, never()).createTransaction(org.mockito.ArgumentMatchers.any(CreateTransactionRequest.class), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createAcceptsMatchingGatewayUserAndPassesIdempotencyKey() {
        CreateTransactionRequest transaction = createRequest();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "10");
        TransactionResponse expected = new TransactionResponse(1L, 10L, 20L, new BigDecimal("50.00"),
                "2026-07-10T00:00:00", TransactionStatus.SUCCESS.name(), "Transaction successful");
        when(service.createTransaction(transaction, 10L, "idem-1")).thenReturn(expected);

        ResponseEntity<?> response = controller.create(transaction, "idem-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void createReturnsUnprocessableEntityForFailedMoneyMovement() {
        CreateTransactionRequest transaction = createRequest();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "10");
        TransactionResponse failed = new TransactionResponse(1L, 10L, 20L, new BigDecimal("9000.00"),
                "2026-07-10T00:00:00", TransactionStatus.FAILED.name(), "Insufficient funds in your wallet");
        when(service.createTransaction(transaction, 10L, "idem-1")).thenReturn(failed);

        ResponseEntity<?> response = controller.create(transaction, "idem-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isSameAs(failed);
    }

    @Test
    void userHistoryRejectsMissingOrMismatchedGatewayUser() {
        ResponseEntity<?> missing = controller.getTransactionsByUser(10L, new MockHttpServletRequest());
        MockHttpServletRequest mismatchRequest = new MockHttpServletRequest();
        mismatchRequest.addHeader("X-User-Id", "99");

        ResponseEntity<?> mismatch = controller.getTransactionsByUser(10L, mismatchRequest);

        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(mismatch.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(service, never()).getTransactionsByUser(10L);
    }

    @Test
    void userHistoryAllowsOwner() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "10");
        when(service.getTransactionsByUser(10L)).thenReturn(List.of(transaction(10L)));

        ResponseEntity<?> response = controller.getTransactionsByUser(10L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).getTransactionsByUser(10L);
    }

    @Test
    void getByIdReturnsNotFoundForMissingTransaction() {
        when(service.getTransactionById(404L)).thenReturn(null);

        ResponseEntity<?> response = controller.getById(404L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getByIdReturnsTransactionWhenFound() {
        Transaction transaction = transaction(10L);
        transaction.setId(1L);
        when(service.getTransactionById(1L)).thenReturn(transaction);

        ResponseEntity<?> response = controller.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(transaction);
    }

    @Test
    void reconciliationRequiresAdminRole() {
        ResponseEntity<?> missing = controller.getTransactionsNeedingReconciliation(new MockHttpServletRequest());
        MockHttpServletRequest userRequest = new MockHttpServletRequest();
        userRequest.addHeader("X-User-Role", "ROLE_USER");

        ResponseEntity<?> user = controller.getTransactionsNeedingReconciliation(userRequest);

        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(user.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(service, never()).getTransactionsNeedingReconciliation();
    }

    @Test
    void reconciliationAllowsAdminRole() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Role", "ROLE_ADMIN");
        Transaction transaction = transaction(10L);
        when(service.getTransactionsNeedingReconciliation()).thenReturn(List.of(transaction));

        ResponseEntity<?> response = controller.getTransactionsNeedingReconciliation(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(List.of(transaction));
    }

    @Test
    void reconcileTransactionRequiresAdminRole() {
        ResponseEntity<?> response = controller.reconcileTransaction(100L, new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(service, never()).reconcileTransaction(100L);
    }

    @Test
    void reconcileTransactionAllowsAdminRole() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Role", "ROLE_ADMIN");
        ReconciliationResponse expected = new ReconciliationResponse(100L, "CAPTURED", "REFUNDED", "REFUND_SENDER", "Refunded captured funds to sender");
        when(service.reconcileTransaction(100L)).thenReturn(expected);

        ResponseEntity<?> response = controller.reconcileTransaction(100L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void reconcileStaleTransactionsAllowsAdminRole() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Role", "ROLE_ADMIN");
        ReconciliationResponse expected = new ReconciliationResponse(100L, "HOLD_PLACED", "FAILED", "RELEASE_HOLD", "Released stale wallet hold");
        when(service.reconcileStaleTransactions()).thenReturn(List.of(expected));

        ResponseEntity<?> response = controller.reconcileStaleTransactions(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(List.of(expected));
    }

    private Transaction transaction(Long senderId) {
        Transaction transaction = new Transaction();
        transaction.setSenderId(senderId);
        transaction.setReceiverId(20L);
        transaction.setAmount(new BigDecimal("50.00"));
        return transaction;
    }

    private CreateTransactionRequest createRequest() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setReceiverPayTag("@receiver");
        request.setAmount(new BigDecimal("50.00"));
        return request;
    }
}
