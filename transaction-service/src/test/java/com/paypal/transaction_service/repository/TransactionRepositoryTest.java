package com.paypal.transaction_service.repository;

import com.paypal.transaction_service.entity.Transaction;
import com.paypal.transaction_service.entity.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository repository;

    @Test
    void visibleHistoryIncludesSenderFailuresButExcludesReceiverFailures() {
        Transaction successfulIncoming = transaction(20L, 10L, "25.00", TransactionStatus.SUCCESS);
        Transaction failedIncoming = transaction(30L, 10L, "9000.00", TransactionStatus.FAILED);
        Transaction failedOutgoing = transaction(10L, 40L, "9000.00", TransactionStatus.FAILED);
        repository.saveAll(List.of(successfulIncoming, failedIncoming, failedOutgoing));

        List<Transaction> visible = repository.findVisibleHistoryByUserId(10L);

        assertThat(visible)
                .contains(successfulIncoming, failedOutgoing)
                .doesNotContain(failedIncoming);
    }

    private Transaction transaction(Long senderId, Long receiverId, String amount, TransactionStatus status) {
        Transaction transaction = new Transaction();
        transaction.setSenderId(senderId);
        transaction.setReceiverId(receiverId);
        transaction.setSenderPayTag("@user" + senderId);
        transaction.setReceiverPayTag("@user" + receiverId);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setStatus(status);
        return transaction;
    }
}
