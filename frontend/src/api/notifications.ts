import { request } from "./http";
import type { Notification, SendNotificationRequest } from "./types";

export function getNotifications(userId: number) {
  return request<Notification[]>(`/api/notifications/${userId}`);
}

export function markNotificationsRead(userId: number) {
  return request<{ updated: number }>(`/api/notifications/${userId}/read`, {
    method: "PATCH"
  });
}

export function sendNotification(payload: SendNotificationRequest) {
  return request<Notification>("/api/notifications", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}
