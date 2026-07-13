package com.paypal.notification_service.controller;

import com.paypal.notification_service.entity.Notification;
import com.paypal.notification_service.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationControllerTest {

    private NotificationService notificationService;
    private NotificationController controller;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        controller = new NotificationController(notificationService);
    }

    @Test
    void adminCanSendNotification() {
        Notification notification = notification(10L, "message");
        when(notificationService.sendNotification(notification)).thenReturn(notification);

        ResponseEntity<?> response = controller.sendNotification(notification, "ROLE_ADMIN");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService).sendNotification(notification);
    }

    @Test
    void nonAdminCannotSendNotification() {
        Notification notification = notification(10L, "message");

        ResponseEntity<?> response = controller.sendNotification(notification, "ROLE_USER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(notificationService, never()).sendNotification(notification);
    }

    @Test
    void userCanReadOwnNotifications() {
        when(notificationService.getNotificationsByUserId(10L)).thenReturn(List.of(notification(10L, "message")));

        ResponseEntity<?> response = controller.getNotificationsByUser(10L, "10", "ROLE_USER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService).getNotificationsByUserId(10L);
    }

    @Test
    void userCanMarkOwnNotificationsRead() {
        when(notificationService.markNotificationsRead(10L)).thenReturn(2);

        ResponseEntity<?> response = controller.markNotificationsRead(10L, "10", "ROLE_USER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new NotificationController.MarkReadResponse(2));
        verify(notificationService).markNotificationsRead(10L);
    }

    @Test
    void userCannotMarkAnotherUsersNotificationsRead() {
        ResponseEntity<?> response = controller.markNotificationsRead(10L, "99", "ROLE_USER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(notificationService, never()).markNotificationsRead(10L);
    }

    @Test
    void adminCanReadAnyUsersNotifications() {
        when(notificationService.getNotificationsByUserId(10L)).thenReturn(List.of(notification(10L, "message")));

        ResponseEntity<?> response = controller.getNotificationsByUser(10L, null, "ROLE_ADMIN");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService).getNotificationsByUserId(10L);
    }

    @Test
    void malformedUserHeaderIsForbiddenForNonAdminRead() {
        ResponseEntity<?> response = controller.getNotificationsByUser(10L, "not-a-number", "ROLE_USER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(notificationService, never()).getNotificationsByUserId(10L);
    }

    @Test
    void userCannotReadAnotherUsersNotifications() {
        ResponseEntity<?> response = controller.getNotificationsByUser(10L, "99", "ROLE_USER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(notificationService, never()).getNotificationsByUserId(10L);
    }

    private Notification notification(Long userId, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setSentAt(LocalDateTime.now());
        return notification;
    }
}
