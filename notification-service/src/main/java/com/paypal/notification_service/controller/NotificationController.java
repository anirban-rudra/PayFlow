package com.paypal.notification_service.controller;

import com.paypal.notification_service.entity.Notification;
import com.paypal.notification_service.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/notifications", "/api/notify"})
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<?> sendNotification(@RequestBody Notification notification,
                                              @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }

        return ResponseEntity.ok(notificationService.sendNotification(notification));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getNotificationsByUser(@PathVariable("userId") Long userId,
                                                    @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
                                                    @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!canAccessUserNotifications(userId, userIdHeader, role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to view these notifications");
        }

        List<Notification> notifications = notificationService.getNotificationsByUserId(userId);
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/{userId}/read")
    public ResponseEntity<?> markNotificationsRead(@PathVariable("userId") Long userId,
                                                   @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
                                                   @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!canAccessUserNotifications(userId, userIdHeader, role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to update these notifications");
        }

        int updated = notificationService.markNotificationsRead(userId);
        return ResponseEntity.ok(new MarkReadResponse(updated));
    }

    private boolean isAdmin(String role) {
        return "ROLE_ADMIN".equals(role);
    }

    private Long parseHeaderUserId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean canAccessUserNotifications(Long userId, String userIdHeader, String role) {
        Long authenticatedUserId = parseHeaderUserId(userIdHeader);
        return isAdmin(role) || userId.equals(authenticatedUserId);
    }

    public record MarkReadResponse(int updated) {
    }
}
