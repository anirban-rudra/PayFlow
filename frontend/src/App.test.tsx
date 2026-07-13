import { screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";
import { createTestToken } from "./test/jwt";
import { renderWithProviders } from "./test/render";

const mutateAsync = vi.fn();

vi.mock("./hooks/usePayflow", () => ({
  useCurrentUser: () => ({
    data: {
      id: 1,
      name: "PayFlow User",
      email: "user@example.com",
      payTag: "@payflowuser"
    }
  }),
  useWallet: () => ({
    data: {
      id: 1,
      userId: 1,
      currency: "INR",
      balance: "0.00",
      availableBalance: "0.00"
    }
  }),
  useTopUpWallet: () => ({
    mutateAsync,
    isPending: false
  }),
  useTransactions: () => ({
    isLoading: false,
    data: []
  }),
  useRewards: () => ({
    data: []
  }),
  useNotifications: () => ({
    data: []
  })
}));

describe("App routes", () => {
  beforeEach(() => {
    mutateAsync.mockReset();
    sessionStorage.setItem("payflow.session", createTestToken({ userId: 1 }));
  });

  it("renders the protected Top Up Wallet page inside the app shell", () => {
    renderWithProviders(<App />, { route: "/app/top-up" });

    expect(screen.getByRole("heading", { name: "Top Up Wallet" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /send money/i })).toHaveAttribute("href", "/app/send");
    expect(screen.getByRole("link", { name: /top up wallet/i })).toHaveClass("active");
  });
});
