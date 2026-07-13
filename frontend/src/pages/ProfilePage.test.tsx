import { screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createTestToken } from "../test/jwt";
import { renderWithProviders } from "../test/render";
import { ProfilePage } from "./ProfilePage";

vi.mock("../hooks/usePayflow", () => ({
  useCurrentUser: () => ({
    data: {
      id: 7,
      name: "Alice Smith",
      email: "alice@example.com",
      payTag: "@alice"
    }
  }),
  useWallet: () => ({
    data: {
      id: 3,
      userId: 7,
      currency: "INR",
      balance: "1200.00",
      availableBalance: "1100.00"
    }
  })
}));

describe("ProfilePage", () => {
  beforeEach(() => {
    sessionStorage.setItem("payflow.session", createTestToken({ userId: 7, email: "alice@example.com" }));
  });

  it("displays the user's PayTag as the public wallet identity", () => {
    renderWithProviders(<ProfilePage />, { route: "/app/profile" });

    expect(screen.getByRole("heading", { name: "Alice Smith" })).toBeInTheDocument();
    expect(screen.getAllByText("@alice")).toHaveLength(2);
    expect(screen.getByText("PayTag")).toBeInTheDocument();
    expect(screen.queryByText(/session status/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/protected/i)).not.toBeInTheDocument();
  });
});
