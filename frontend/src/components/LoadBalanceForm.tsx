import { CheckCircle2, WalletCards } from "lucide-react";
import { type FormEvent, useState } from "react";
import { ApiError } from "../api/http";
import { useTopUpWallet, useWallet } from "../hooks/usePayflow";
import { formatMoney, parseMoney } from "../lib/money";
import { useAuth } from "../state/auth";
import { Button, ErrorBanner } from "./ui";

const presets = ["500.00", "1000.00", "2500.00"];

export function LoadBalanceForm() {
  const { user } = useAuth();
  const wallet = useWallet(user?.userId);
  const topUp = useTopUpWallet(user?.userId);
  const currency = wallet.data?.currency ?? "INR";
  const [amount, setAmount] = useState("");
  const [error, setError] = useState("");
  const [receipt, setReceipt] = useState<{
    amount: string;
    availableBalance: string;
  } | null>(null);

  const updateAmount = (value: string) => {
    setAmount(value);
    setError("");
    setReceipt(null);
  };

  const clearTopUpFeedback = () => {
    setError("");
    setReceipt(null);
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    setReceipt(null);

    if (!user) {
      setError("Sign in again before loading balance.");
      return;
    }

    try {
      const money = parseMoney(amount);
      const response = await topUp.mutateAsync({
        currency,
        amount: money.toFixed(2)
      });
      setReceipt({
        amount: formatMoney(money, currency),
        availableBalance: formatMoney(response.availableBalance, response.currency)
      });
      setAmount("");
    } catch (err) {
      setError(err instanceof ApiError || err instanceof Error ? err.message : "Could not load balance.");
    }
  };

  return (
    <form className="form-stack compact-form" onSubmit={submit}>
      <ErrorBanner message={error} />
      {receipt && (
        <section className="transfer-receipt" aria-label="Top-up receipt">
          <div className="receipt-status">
            <CheckCircle2 size={20} />
            <div>
              <strong>Money added</strong>
              <span>Available balance updated</span>
            </div>
          </div>
          <dl>
            <div>
              <dt>Amount</dt>
              <dd>{receipt.amount}</dd>
            </div>
            <div>
              <dt>Available</dt>
              <dd>{receipt.availableBalance}</dd>
            </div>
          </dl>
        </section>
      )}
      <div className="balance-strip">
        <span>Current available</span>
        <strong>{wallet.data ? formatMoney(wallet.data.availableBalance, currency) : "--"}</strong>
      </div>
      <div className="preset-row" aria-label="Common load amounts">
        {presets.map((preset) => (
          <button className="preset-button" key={preset} type="button" onClick={() => updateAmount(preset)}>
            {formatMoney(preset, currency)}
          </button>
        ))}
      </div>
      <label>
        <span>Amount</span>
        <input inputMode="decimal" placeholder="0.00" value={amount} onChange={(event) => updateAmount(event.target.value)} onFocus={clearTopUpFeedback} />
      </label>
      <Button loading={topUp.isPending} disabled={topUp.isPending} type="submit">
        <WalletCards size={17} />
        Add money
      </Button>
    </form>
  );
}
