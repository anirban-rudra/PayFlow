import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createTestToken } from "../test/jwt";
import { renderWithProviders } from "../test/render";
import { LoadBalanceForm } from "./LoadBalanceForm";

const mutateAsync = vi.fn();

vi.mock("../hooks/usePayflow", () => ({
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
  })
}));

describe("LoadBalanceForm", () => {
  beforeEach(() => {
    mutateAsync.mockReset();
    mutateAsync.mockResolvedValue({
      id: 1,
      userId: 1,
      currency: "INR",
      balance: "500.00",
      availableBalance: "500.00"
    });
    sessionStorage.setItem("payflow.session", createTestToken({ userId: 1 }));
  });

  it("rejects invalid amounts before calling the API", async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoadBalanceForm />);

    await user.click(screen.getByRole("button", { name: /add money/i }));

    expect(await screen.findByText("Enter a valid amount")).toBeInTheDocument();
    expect(mutateAsync).not.toHaveBeenCalled();
  });

  it("clears stale amount errors when the amount changes", async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoadBalanceForm />);

    const amountInput = screen.getByRole("textbox", { name: "Amount" });
    await user.click(screen.getByRole("button", { name: /add money/i }));

    expect(await screen.findByText("Enter a valid amount")).toBeInTheDocument();

    await user.type(amountInput, "500");

    expect(screen.queryByText("Enter a valid amount")).not.toBeInTheDocument();
  });

  it("clears stale amount errors when the amount field is focused", async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoadBalanceForm />);

    const amountInput = screen.getByRole("textbox", { name: "Amount" });
    await user.click(screen.getByRole("button", { name: /add money/i }));

    expect(await screen.findByText("Enter a valid amount")).toBeInTheDocument();

    await user.click(amountInput);

    expect(screen.queryByText("Enter a valid amount")).not.toBeInTheDocument();
  });

  it("submits a normalized top-up payload", async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoadBalanceForm />);

    await user.type(screen.getByRole("textbox", { name: "Amount" }), "500");
    await user.click(screen.getByRole("button", { name: /add money/i }));

    expect(mutateAsync).toHaveBeenCalledWith({
      currency: "INR",
      amount: "500.00"
    });
    const receipt = await screen.findByRole("region", { name: /top-up receipt/i });
    expect(within(receipt).getAllByText("₹ 500.00")).toHaveLength(2);
  });
});
