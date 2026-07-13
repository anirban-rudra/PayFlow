import { zodResolver } from "@hookform/resolvers/zod";
import Decimal from "decimal.js";
import { CheckCircle2, ShieldCheck } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { ApiError } from "../api/http";
import { resolveTransferTarget } from "../api/users";
import type { TransferTargetResponse } from "../api/types";
import { useSendMoney, useWallet } from "../hooks/usePayflow";
import { createIdempotencyKey } from "../lib/idempotency";
import { formatTransactionReference } from "../lib/identifiers";
import { formatMoney, parseMoney } from "../lib/money";
import { sendMoneySchema, type SendMoneyFormValues } from "../lib/validation";
import { useAuth } from "../state/auth";
import { Button, ErrorBanner, FieldError, Modal } from "./ui";

interface SendMoneyFormProps {
  compact?: boolean;
}

export function SendMoneyForm({ compact = false }: SendMoneyFormProps) {
  const { user } = useAuth();
  const wallet = useWallet(user?.userId);
  const sendMoney = useSendMoney(user?.userId);
  const [pendingValues, setPendingValues] = useState<SendMoneyFormValues | null>(null);
  const [pendingTarget, setPendingTarget] = useState<TransferTargetResponse | null>(null);
  const [receipt, setReceipt] = useState<{
    reference: string;
    amount: string;
    recipient: string;
    payTag: string;
    status: string;
  } | null>(null);
  const [error, setError] = useState("");
  const currency = wallet.data?.currency ?? "INR";

  const {
    register,
    clearErrors,
    handleSubmit,
    reset,
    setError: setFieldError,
    formState: { errors, isSubmitting }
  } = useForm<SendMoneyFormValues>({
    resolver: zodResolver(sendMoneySchema),
    reValidateMode: "onSubmit",
    shouldFocusError: false,
    defaultValues: {
      receiverPayTag: "",
      amount: "",
      note: ""
    }
  });

  const clearTransferFeedback = (field?: keyof SendMoneyFormValues) => {
    setError("");
    setReceipt(null);
    setPendingTarget(null);
    setPendingValues(null);
    if (field) {
      clearErrors(field);
    }
  };

  const receiverPayTagField = register("receiverPayTag", {
    onChange: () => clearTransferFeedback("receiverPayTag")
  });
  const amountField = register("amount", {
    onChange: () => clearTransferFeedback("amount")
  });
  const noteField = register("note", {
    onChange: () => clearTransferFeedback("note")
  });

  const review = async (values: SendMoneyFormValues) => {
    setError("");
    setReceipt(null);

    const requestedAmount = parseMoney(values.amount);
    if (wallet.data) {
      const availableBalance = new Decimal(wallet.data.availableBalance);
      if (requestedAmount.greaterThan(availableBalance)) {
        setFieldError("amount", {
          message: `Insufficient balance. Available ${formatMoney(availableBalance, currency)}`
        });
        setPendingTarget(null);
        setPendingValues(null);
        return;
      }
    }

    try {
      const target = await resolveTransferTarget(values.receiverPayTag);
      setPendingTarget(target);
      setPendingValues({ ...values, receiverPayTag: target.payTag });
    } catch (err) {
      const message = err instanceof ApiError && err.status === 404 ? "Recipient PayTag was not found" : err instanceof Error ? err.message : "Could not verify recipient";
      setFieldError("receiverPayTag", { message });
      setPendingTarget(null);
      setPendingValues(null);
      return;
    }
  };

  const confirm = async () => {
    if (!pendingValues || !user) {
      return;
    }

    setError("");

    try {
      const money = parseMoney(pendingValues.amount);
      const response = await sendMoney.mutateAsync({
        idempotencyKey: createIdempotencyKey(),
        payload: {
          receiverPayTag: pendingValues.receiverPayTag,
          amount: money.toFixed(2)
        }
      });
      if (response.status !== "SUCCESS") {
        setError(response.message || response.failureReason || "Transfer failed");
        setPendingValues(null);
        return;
      }
      setReceipt({
        reference: formatTransactionReference(response.id, response.publicReference),
        amount: formatMoney(money, currency),
        recipient: pendingTarget?.displayName ?? pendingValues.receiverPayTag,
        payTag: pendingValues.receiverPayTag,
        status: response.status
      });
      setPendingValues(null);
      reset();
    } catch (err) {
      setError(err instanceof ApiError || err instanceof Error ? err.message : "Transfer failed");
      setPendingValues(null);
    }
  };

  const pendingAmount = pendingValues ? parseMoney(pendingValues.amount) : null;

  return (
    <>
      <form className={`form-stack ${compact ? "compact-form" : ""}`} onSubmit={handleSubmit(review)}>
        <ErrorBanner message={error} />
        {receipt && (
          <section className="transfer-receipt" aria-label="Transfer receipt">
            <div className="receipt-status">
              <CheckCircle2 size={20} />
              <div>
                <strong>Transfer completed</strong>
                <span>{receipt.reference}</span>
              </div>
            </div>
            <dl>
              <div>
                <dt>Amount</dt>
                <dd>{receipt.amount}</dd>
              </div>
              <div>
                <dt>Recipient</dt>
                <dd>{receipt.recipient}</dd>
              </div>
              <div>
                <dt>PayTag</dt>
                <dd>{receipt.payTag}</dd>
              </div>
              <div>
                <dt>Status</dt>
                <dd>{receipt.status}</dd>
              </div>
            </dl>
          </section>
        )}
        <div className="balance-strip">
          <span>Available</span>
          <strong>{wallet.data ? formatMoney(wallet.data.availableBalance, currency) : "--"}</strong>
        </div>
        <label>
          <span>Recipient PayTag</span>
          <input autoComplete="off" placeholder="@recipient" {...receiverPayTagField} onFocus={() => clearTransferFeedback("receiverPayTag")} />
          <FieldError message={errors.receiverPayTag?.message} />
        </label>
        <label>
          <span>Amount</span>
          <input inputMode="decimal" placeholder="0.00" {...amountField} onFocus={() => clearTransferFeedback("amount")} />
          <FieldError message={errors.amount?.message} />
        </label>
        {!compact && (
          <label>
            <span>Note</span>
            <input placeholder="Optional" {...noteField} onFocus={() => clearTransferFeedback("note")} />
            <FieldError message={errors.note?.message} />
          </label>
        )}
        <Button loading={isSubmitting || sendMoney.isPending} disabled={sendMoney.isPending} type="submit">
          {sendMoney.isPending ? "Processing transfer" : "Review transfer"}
        </Button>
      </form>

      {pendingValues && pendingAmount && pendingTarget && (
        <Modal
          title="Confirm transfer"
          onClose={() => setPendingValues(null)}
          footer={
            <>
              <Button type="button" variant="secondary" disabled={sendMoney.isPending} onClick={() => setPendingValues(null)}>
                Cancel
              </Button>
              <Button type="button" loading={sendMoney.isPending} onClick={confirm}>
                Confirm and send
              </Button>
            </>
          }
        >
          <div className="confirmation-panel modal-confirmation">
            <ShieldCheck size={20} />
            <div>
              <strong>{formatMoney(pendingAmount, currency)} to {pendingTarget.displayName}</strong>
              <span>{pendingTarget.payTag}</span>
              <span>{pendingValues.note || "No note attached"}</span>
            </div>
          </div>
        </Modal>
      )}
    </>
  );
}
