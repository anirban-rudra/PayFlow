import { useEffect } from "react";
import { Bell } from "lucide-react";
import { Card, EmptyState, ErrorBanner, PageHeader } from "../components/ui";
import { useMarkNotificationsRead, useNotifications } from "../hooks/usePayflow";
import { formatDateTime } from "../lib/time";
import { useAuth } from "../state/auth";

export function NotificationsPage() {
  const { user } = useAuth();
  const notifications = useNotifications(user?.userId);
  const markRead = useMarkNotificationsRead(user?.userId);
  const { mutate: markNotificationsRead, isPending: isMarkingRead } = markRead;
  const hasUnreadNotifications = (notifications.data ?? []).some((notification) => !notification.read && !notification.readAt);

  useEffect(() => {
    if (user?.userId && hasUnreadNotifications && !isMarkingRead) {
      markNotificationsRead();
    }
  }, [hasUnreadNotifications, isMarkingRead, markNotificationsRead, user?.userId]);

  return (
    <div className="page-stack narrow-page">
      <PageHeader title="Notifications" />
      <ErrorBanner message={notifications.error ? "Could not load notifications." : undefined} />
      <Card>
        <div className="notification-list">
          {[...(notifications.data ?? [])]
            .sort((a, b) => new Date(b.sentAt ?? "").getTime() - new Date(a.sentAt ?? "").getTime())
            .map((notification) => (
              <div className="notification-row" key={`${notification.id ?? notification.message}-${notification.sentAt ?? ""}`}>
                <div className="activity-icon info">
                  <Bell size={17} />
                </div>
                <div>
                  <strong>{notification.message}</strong>
                  <span>{formatDateTime(notification.sentAt)}</span>
                </div>
              </div>
            ))}
          {!notifications.isLoading && (notifications.data ?? []).length === 0 && <EmptyState title="No notifications" />}
        </div>
      </Card>
    </div>
  );
}
