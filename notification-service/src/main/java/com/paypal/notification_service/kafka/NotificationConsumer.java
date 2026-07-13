package com.paypal.notification_service.kafka;

import com.paypal.notification_service.entity.Notification;
import com.paypal.notification_service.entity.Transaction;
import com.paypal.notification_service.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.time.LocalDateTime;

@Component
public class NotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationRepository notificationRepository;

    public NotificationConsumer(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @KafkaListener(topics = "txn-initiated", groupId = "notification-group")
    public void consumeTransaction(Transaction transaction) {
        LocalDateTime sentAt = LocalDateTime.now();

        Notification senderNotification = new Notification();
        senderNotification.setUserId(transaction.getSenderId());
        senderNotification.setMessage("Payment of INR " + transaction.getAmount() + " sent to " + receiverLabel(transaction));
        senderNotification.setSentAt(sentAt);

        Notification receiverNotification = new Notification();
        receiverNotification.setUserId(transaction.getReceiverId());
        receiverNotification.setMessage("Payment of INR " + transaction.getAmount() + " received from a PayFlow user");
        receiverNotification.setSentAt(sentAt);

        notificationRepository.saveAll(List.of(senderNotification, receiverNotification));
        log.info("Notifications saved for transaction {}", transaction.getId());
    }

    private String receiverLabel(Transaction transaction) {
        String payTag = transaction.getReceiverPayTag();
        if (payTag != null && !payTag.isBlank()) {
            return payTag;
        }
        return "a PayFlow user";
    }
}
