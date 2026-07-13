package com.paypal.notification_service.service;

import com.paypal.notification_service.entity.Notification;
import com.paypal.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceImplTest {

    @Test
    void sendNotificationSetsFreshTimestampAndPersistsNotification() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationServiceImpl service = new NotificationServiceImpl(repository);
        Notification notification = notification(10L, "message");
        notification.setSentAt(LocalDateTime.parse("2020-01-01T00:00:00"));
        when(repository.save(notification)).thenReturn(notification);

        Notification saved = service.sendNotification(notification);

        assertThat(saved.getSentAt()).isAfter(LocalDateTime.parse("2020-01-01T00:00:00"));
        verify(repository).save(notification);
    }

    @Test
    void getNotificationsByUserIdDelegatesToRepository() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationServiceImpl service = new NotificationServiceImpl(repository);
        Notification notification = notification(10L, "message");
        when(repository.findByUserId(10L)).thenReturn(List.of(notification));

        assertThat(service.getNotificationsByUserId(10L)).containsExactly(notification);
    }

    @Test
    void markNotificationsReadUpdatesUnreadRowsForUser() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationServiceImpl service = new NotificationServiceImpl(repository);
        when(repository.markUnreadNotificationsRead(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(2);

        assertThat(service.markNotificationsRead(10L)).isEqualTo(2);
        verify(repository).markUnreadNotificationsRead(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    private Notification notification(Long userId, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        return notification;
    }
}
