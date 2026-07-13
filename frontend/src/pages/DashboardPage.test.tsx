import { screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createTestToken } from "../test/jwt";
import { renderWithProviders } from "../test/render";
import { DashboardPage } from "./DashboardPage";

const mutateAsync = vi.fn();
const transactionFixtures = vi.hoisted(() => ({
  data: [
    {
      id: 10,
      publicReference: "PF-TXN-SUCCESS",
      senderId: 1,
      receiverId: 2,
      receiverPayTag: "@receiver",
      amount: "25.00",
      timestamp: "2026-07-10T12:00:00",
      status: "SUCCESS"
    },
    {
      id: 11,
      publicReference: "PF-TXN-INFAILED",
      senderId: 99,
      receiverId: 1,
      senderPayTag: "@failedsender",
      receiverPayTag: "@current",
      amount: "9000.00",
      timestamp: "2026-07-10T12:05:00",
      status: "FAILED"
    },
    {
      id: 12,
      publicReference: "PF-TXN-INCOMING",
      senderId: 77,
      receiverId: 1,
      senderPayTag: "@sender",
      receiverPayTag: "@current",
      amount: "40.00",
      timestamp: "2026-07-10T12:10:00",
      status: "SUCCESS"
    }
  ]
}));

vi.mock("../api/users", () => ({
  resolveTransferTarget: vi.fn()
}));

vi.mock("../hooks/usePayflow", () => ({
  useWallet: () => ({
    data: {
      id: 1,
      userId: 1,
      currency: "INR",
      balance: "500.00",
      availableBalance: "500.00"
    }
  }),
  useTransactions: () => ({
    isLoading: false,
    data: transactionFixtures.data
  }),
  useRewards: () => ({
    data: []
  }),
  useNotifications: () => ({
    data: [
      {
        id: 1,
        userId: 1,
        message: "Unread alert",
        sentAt: "2026-07-10T12:20:00",
        read: false,
        readAt: null
      },
      {
        id: 2,
        userId: 1,
        message: "Already read alert",
        sentAt: "2026-07-10T12:25:00",
        read: true,
        readAt: "2026-07-10T12:30:00"
      }
    ]
  }),
  useTopUpWallet: () => ({
    mutateAsync,
    isPending: false
  }),
  useSendMoney: () => ({
    mutateAsync,
    isPending: false
  })
}));

describe("DashboardPage", () => {
  beforeEach(() => {
    mutateAsync.mockReset();
    sessionStorage.setItem("payflow.session", createTestToken({ userId: 1 }));
  });

  it("uses recent transaction wording and exposes focused expand actions", () => {
    renderWithProviders(<DashboardPage />, { route: "/app/dashboard" });

    expect(screen.getByRole("heading", { name: "Recent transactions" })).toBeInTheDocument();
    expect(screen.queryByText(/settlement states/i)).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: /open top up wallet/i })).toHaveAttribute("href", "/app/top-up");
    expect(screen.getByRole("link", { name: /open send money/i })).toHaveAttribute("href", "/app/send");
    expect(screen.getByText("From @sender")).toBeInTheDocument();
    expect(screen.queryByText("From PayFlow user")).not.toBeInTheDocument();
  });

  it("does not show failed incoming attempts in recent transactions", () => {
    renderWithProviders(<DashboardPage />, { route: "/app/dashboard" });

    expect(screen.queryByText("₹ 9,000.00")).not.toBeInTheDocument();
  });

  it("counts only unread alerts", () => {
    renderWithProviders(<DashboardPage />, { route: "/app/dashboard" });

    const unreadLabel = screen.getByText("Unread alerts");
    expect(within(unreadLabel.closest(".stat-card")!).getByText("1")).toBeInTheDocument();
  });
});
