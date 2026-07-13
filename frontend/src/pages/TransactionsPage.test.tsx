import { screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createTestToken } from "../test/jwt";
import { renderWithProviders } from "../test/render";
import { TransactionsPage } from "./TransactionsPage";

const transactionFixtures = vi.hoisted(() => ({
  data: [
    {
      id: 2,
      publicReference: "PF-TXN-ABC123",
      senderId: 1,
      receiverId: 3,
      receiverPayTag: "@receiver",
      amount: "25.00",
      timestamp: "2026-07-10T12:00:00",
      status: "SUCCESS"
    },
    {
      id: 3,
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
      id: 4,
      publicReference: "PF-TXN-OUTFAILED",
      senderId: 1,
      receiverId: 99,
      receiverPayTag: "@missing",
      amount: "9000.00",
      timestamp: "2026-07-10T12:10:00",
      status: "FAILED"
    },
    {
      id: 5,
      publicReference: "PF-TXN-INCOMING",
      senderId: 77,
      receiverId: 1,
      senderPayTag: "@sender",
      receiverPayTag: "@current",
      amount: "40.00",
      timestamp: "2026-07-10T12:15:00",
      status: "SUCCESS"
    }
  ]
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
  })
}));

describe("TransactionsPage", () => {
  beforeEach(() => {
    sessionStorage.setItem("payflow.session", createTestToken({ userId: 1 }));
  });

  it("uses product transaction references and PayTag counterparties", () => {
    renderWithProviders(<TransactionsPage />, { route: "/app/transactions" });

    expect(screen.getAllByText("PF-TXN-ABC123")).toHaveLength(2);
    expect(screen.getAllByText("@receiver")).toHaveLength(2);
    expect(screen.getAllByText("@sender")).toHaveLength(2);
    expect(screen.queryByText("PayFlow user")).not.toBeInTheDocument();
    expect(screen.queryByText("#2")).not.toBeInTheDocument();
    expect(screen.queryByText("TXN-000002")).not.toBeInTheDocument();
    expect(screen.queryByText(/User #/i)).not.toBeInTheDocument();
    expect(screen.getByPlaceholderText("Search transaction, PayTag, status")).toBeInTheDocument();
  });

  it("hides failed incoming attempts but keeps failed outgoing attempts", () => {
    renderWithProviders(<TransactionsPage />, { route: "/app/transactions" });

    expect(screen.queryByText("PF-TXN-INFAILED")).not.toBeInTheDocument();
    expect(screen.getAllByText("PF-TXN-OUTFAILED")).toHaveLength(2);
  });
});
