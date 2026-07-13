import { screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createTestToken } from "../test/jwt";
import { renderWithProviders } from "../test/render";
import { RewardsPage } from "./RewardsPage";

vi.mock("../hooks/usePayflow", () => ({
  useRewards: () => ({
    isLoading: false,
    data: [
      {
        id: 4,
        userId: 1,
        transactionId: 2,
        points: "1.2500",
        sentAt: "2026-07-10T12:00:00"
      }
    ]
  })
}));

describe("RewardsPage", () => {
  beforeEach(() => {
    sessionStorage.setItem("payflow.session", createTestToken({ userId: 1 }));
  });

  it("explains reward eligibility and formats references for users", () => {
    renderWithProviders(<RewardsPage />, { route: "/app/rewards" });

    expect(screen.getByText("5%")).toBeInTheDocument();
    expect(screen.getByText("INR 5.00")).toBeInTheDocument();
    expect(screen.getByText(/points are issued to the sender/i)).toBeInTheDocument();
    expect(screen.getAllByText("RWD-000004")).toHaveLength(2);
    expect(screen.getAllByText("TXN-000002")).toHaveLength(2);
    expect(screen.queryByText("#4")).not.toBeInTheDocument();
    expect(screen.queryByText("#2")).not.toBeInTheDocument();
  });
});
