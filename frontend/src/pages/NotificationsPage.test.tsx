import { screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createTestToken } from "../test/jwt";
import { renderWithProviders } from "../test/render";
import { NotificationsPage } from "./NotificationsPage";

type NotificationFixture = {
  id: number;
  userId: number;
  message: string;
  sentAt: string;
  read: boolean;
  readAt: string | null;
};

const markRead = vi.hoisted(() => vi.fn());
const notificationState = vi.hoisted(() => ({
  data: [
    {
      id: 1,
      userId: 1,
      message: "Payment received",
      sentAt: "2026-07-10T12:20:00",
      read: false,
      readAt: null
    }
  ] as NotificationFixture[]
}));

vi.mock("../hooks/usePayflow", () => ({
  useNotifications: () => ({
    isLoading: false,
    data: notificationState.data
  }),
  useMarkNotificationsRead: () => ({
    mutate: markRead,
    isPending: false
  })
}));

describe("NotificationsPage", () => {
  beforeEach(() => {
    markRead.mockReset();
    notificationState.data = [
      {
        id: 1,
        userId: 1,
        message: "Payment received",
        sentAt: "2026-07-10T12:20:00",
        read: false,
        readAt: null
      }
    ];
    sessionStorage.setItem("payflow.session", createTestToken({ userId: 1 }));
  });

  it("marks unread notifications as read when opened", async () => {
    renderWithProviders(<NotificationsPage />, { route: "/app/notifications" });

    expect(screen.getByText("Payment received")).toBeInTheDocument();
    await waitFor(() => expect(markRead).toHaveBeenCalledTimes(1));
  });

  it("does not mark notifications read when they are already read", async () => {
    notificationState.data = [
      {
        id: 1,
        userId: 1,
        message: "Payment received",
        sentAt: "2026-07-10T12:20:00",
        read: true,
        readAt: "2026-07-10T12:30:00"
      }
    ];

    renderWithProviders(<NotificationsPage />, { route: "/app/notifications" });

    expect(screen.getByText("Payment received")).toBeInTheDocument();
    await waitFor(() => expect(markRead).not.toHaveBeenCalled());
  });
});
