import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../api/http";
import { createTestToken } from "../test/jwt";
import { renderWithProviders } from "../test/render";
import { SendMoneyForm } from "./SendMoneyForm";

const mutateAsync = vi.fn();
const resolveTransferTarget = vi.fn();

vi.mock("../hooks/usePayflow", () => ({
  useWallet: () => ({
    data: {
      id: 1,
      userId: 1,
      currency: "INR",
      balance: "1000.00",
      availableBalance: "900.00"
    }
  }),
  useSendMoney: () => ({
    mutateAsync,
    isPending: false
  })
}));

vi.mock("../api/users", () => ({
  resolveTransferTarget: (payTag: string) => resolveTransferTarget(payTag)
}));

describe("SendMoneyForm", () => {
  beforeEach(() => {
    mutateAsync.mockReset();
    resolveTransferTarget.mockReset();
    resolveTransferTarget.mockResolvedValue({ displayName: "Receiver", payTag: "@receiver" });
    mutateAsync.mockResolvedValue({ id: 12, publicReference: "PF-TXN-ABCD1234", status: "SUCCESS", message: "Transfer completed" });
    sessionStorage.setItem("payflow.session", createTestToken({ userId: 1 }));
  });

  it("shows field validation errors before opening confirmation", async () => {
    const user = userEvent.setup();
    renderWithProviders(<SendMoneyForm />);

    await user.click(screen.getByRole("button", { name: /review transfer/i }));

    expect(await screen.findByText(/PayTag must start with @/i)).toBeInTheDocument();
    expect(screen.getByText("Use a positive amount with up to 2 decimals")).toBeInTheDocument();
    expect(mutateAsync).not.toHaveBeenCalled();
  });

  it("clears empty-field validation errors when the user focuses each field", async () => {
    const user = userEvent.setup();
    renderWithProviders(<SendMoneyForm compact />);

    await user.click(screen.getByRole("button", { name: /review transfer/i }));

    expect(await screen.findByText(/PayTag must start with @/i)).toBeInTheDocument();
    expect(screen.getByText("Use a positive amount with up to 2 decimals")).toBeInTheDocument();

    await user.click(screen.getByLabelText(/recipient paytag/i));
    expect(screen.queryByText(/PayTag must start with @/i)).not.toBeInTheDocument();
    expect(screen.getByText("Use a positive amount with up to 2 decimals")).toBeInTheDocument();

    await user.click(screen.getByLabelText(/amount/i));
    expect(screen.queryByText("Use a positive amount with up to 2 decimals")).not.toBeInTheDocument();
  });

  it("shows an error when the recipient cannot be resolved", async () => {
    const user = userEvent.setup();
    resolveTransferTarget.mockRejectedValueOnce(new ApiError({ status: 404, message: "Recipient not found" }));
    renderWithProviders(<SendMoneyForm />);

    await user.type(screen.getByLabelText(/recipient paytag/i), "@missing");
    await user.type(screen.getByLabelText(/amount/i), "10.00");
    await user.click(screen.getByRole("button", { name: /review transfer/i }));

    expect(await screen.findByText("Recipient PayTag was not found")).toBeInTheDocument();
    expect(mutateAsync).not.toHaveBeenCalled();
  });

  it("clears stale recipient errors as soon as the field changes", async () => {
    const user = userEvent.setup();
    resolveTransferTarget.mockRejectedValueOnce(new ApiError({ status: 404, message: "Recipient not found" }));
    renderWithProviders(<SendMoneyForm />);

    const payTagInput = screen.getByLabelText(/recipient paytag/i);
    await user.type(payTagInput, "@missing");
    await user.type(screen.getByLabelText(/amount/i), "10.00");
    await user.click(screen.getByRole("button", { name: /review transfer/i }));

    expect(await screen.findByText("Recipient PayTag was not found")).toBeInTheDocument();

    await user.clear(payTagInput);

    expect(screen.queryByText("Recipient PayTag was not found")).not.toBeInTheDocument();
  });

  it("submits the expected transfer payload after confirmation", async () => {
    const user = userEvent.setup();
    renderWithProviders(<SendMoneyForm />);

    await user.type(screen.getByLabelText(/recipient paytag/i), "@receiver");
    await user.type(screen.getByLabelText(/amount/i), "15.25");
    await user.type(screen.getByLabelText(/note/i), "Lunch");
    await user.click(screen.getByRole("button", { name: /review transfer/i }));

    expect(await screen.findByRole("dialog", { name: /confirm transfer/i })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /confirm and send/i }));

    expect(mutateAsync).toHaveBeenCalledWith({
      idempotencyKey: expect.any(String),
      payload: {
        receiverPayTag: "@receiver",
        amount: "15.25"
      }
    });
    expect(await screen.findByRole("region", { name: /transfer receipt/i })).toBeInTheDocument();
    expect(screen.getByText("PF-TXN-ABCD1234")).toBeInTheDocument();
    expect(screen.getByText("₹ 15.25")).toBeInTheDocument();
    expect(screen.getByText("@receiver")).toBeInTheDocument();
  });

  it("blocks review when the amount is greater than the available balance", async () => {
    const user = userEvent.setup();
    renderWithProviders(<SendMoneyForm />);

    await user.type(screen.getByLabelText(/recipient paytag/i), "@receiver");
    await user.type(screen.getByLabelText(/amount/i), "901.00");
    await user.click(screen.getByRole("button", { name: /review transfer/i }));

    expect(await screen.findByText("Insufficient balance. Available ₹ 900.00")).toBeInTheDocument();
    expect(resolveTransferTarget).not.toHaveBeenCalled();
    expect(mutateAsync).not.toHaveBeenCalled();
  });

  it("clears insufficient balance errors as soon as the amount changes", async () => {
    const user = userEvent.setup();
    renderWithProviders(<SendMoneyForm />);

    const amountInput = screen.getByLabelText(/amount/i);
    await user.type(screen.getByLabelText(/recipient paytag/i), "@receiver");
    await user.type(amountInput, "901.00");
    await user.click(screen.getByRole("button", { name: /review transfer/i }));

    expect(await screen.findByText("Insufficient balance. Available ₹ 900.00")).toBeInTheDocument();

    await user.clear(amountInput);

    expect(screen.queryByText("Insufficient balance. Available ₹ 900.00")).not.toBeInTheDocument();
  });

  it("shows an error instead of a completed receipt when the transfer response is failed", async () => {
    const user = userEvent.setup();
    mutateAsync.mockResolvedValueOnce({
      id: 12,
      publicReference: "PF-TXN-FAILED1",
      status: "FAILED",
      message: "Insufficient funds in your wallet"
    });
    renderWithProviders(<SendMoneyForm />);

    await user.type(screen.getByLabelText(/recipient paytag/i), "@receiver");
    await user.type(screen.getByLabelText(/amount/i), "15.25");
    await user.click(screen.getByRole("button", { name: /review transfer/i }));
    await user.click(await screen.findByRole("button", { name: /confirm and send/i }));

    expect(await screen.findByText("Insufficient funds in your wallet")).toBeInTheDocument();
    expect(screen.queryByText(/transfer completed/i)).not.toBeInTheDocument();
    expect(screen.queryByText("PF-TXN-FAILED1")).not.toBeInTheDocument();
  });
});
