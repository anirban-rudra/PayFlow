import { useMemo, useState } from "react";
import { Search } from "lucide-react";
import { Card, EmptyState, ErrorBanner, PageHeader, StatusBadge } from "../components/ui";
import { useTransactions, useWallet } from "../hooks/usePayflow";
import { formatCounterparty, formatTransactionReference } from "../lib/identifiers";
import { formatMoney } from "../lib/money";
import { formatDateTime } from "../lib/time";
import { isVisibleTransactionForUser } from "../lib/transactions";
import { useAuth } from "../state/auth";

export function TransactionsPage() {
  const { user } = useAuth();
  const transactions = useTransactions(user?.userId);
  const wallet = useWallet(user?.userId);
  const [status, setStatus] = useState("ALL");
  const [direction, setDirection] = useState("ALL");
  const [search, setSearch] = useState("");
  const currency = wallet.data?.currency ?? "INR";

  const filtered = useMemo(() => {
    return [...(transactions.data ?? [])]
      .filter((transaction) => isVisibleTransactionForUser(transaction, user?.userId))
      .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
      .filter((transaction) => status === "ALL" || transaction.status === status)
      .filter((transaction) => {
        if (direction === "IN") {
          return transaction.receiverId === user?.userId;
        }
        if (direction === "OUT") {
          return transaction.senderId === user?.userId;
        }
        return true;
      })
      .filter((transaction) => {
        const query = search.trim().toLowerCase();
        if (!query) {
          return true;
        }
        return [
          transaction.id,
          transaction.senderId,
          transaction.receiverId,
          transaction.senderPayTag ?? "",
          transaction.receiverPayTag ?? "",
          transaction.status,
          transaction.message ?? ""
        ]
          .join(" ")
          .toLowerCase()
          .includes(query);
      });
  }, [direction, search, status, transactions.data, user?.userId]);

  return (
    <div className="page-stack">
      <PageHeader title="Transactions" />
      <ErrorBanner message={transactions.error ? "Could not load transactions." : undefined} />

      <Card>
        <div className="filter-bar">
          <label className="search-field">
            <Search size={17} />
            <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search transaction, PayTag, status" />
          </label>
          <select value={status} onChange={(event) => setStatus(event.target.value)}>
            <option value="ALL">All statuses</option>
            <option value="SUCCESS">Success</option>
            <option value="PENDING">Pending</option>
            <option value="FAILED">Failed</option>
          </select>
          <select value={direction} onChange={(event) => setDirection(event.target.value)}>
            <option value="ALL">All directions</option>
            <option value="IN">Incoming</option>
            <option value="OUT">Outgoing</option>
          </select>
        </div>

        <div className="desktop-table">
          <table>
            <thead>
              <tr>
                <th>Transaction</th>
                <th>Direction</th>
                <th>Counterparty</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((transaction) => {
                const incoming = transaction.receiverId === user?.userId;
                const counterparty = incoming ? formatCounterparty(transaction.senderPayTag) : formatCounterparty(transaction.receiverPayTag);
                return (
                  <tr key={transaction.id}>
                    <td>{formatTransactionReference(transaction.id, transaction.publicReference)}</td>
                    <td>{incoming ? "Incoming" : "Outgoing"}</td>
                    <td>{counterparty}</td>
                    <td>{formatMoney(transaction.amount, currency)}</td>
                    <td>
                      <StatusBadge status={transaction.status} />
                    </td>
                    <td>{formatDateTime(transaction.timestamp)}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        <div className="mobile-list">
          {filtered.map((transaction) => {
            const incoming = transaction.receiverId === user?.userId;
            const counterparty = incoming ? formatCounterparty(transaction.senderPayTag) : formatCounterparty(transaction.receiverPayTag);
            return (
              <div className="mobile-record" key={transaction.id}>
                <div>
                  <strong>{incoming ? "Incoming" : "Outgoing"}</strong>
                  <span>{formatTransactionReference(transaction.id, transaction.publicReference)}</span>
                </div>
                <div>
                  <span>{counterparty}</span>
                  <strong>{formatMoney(transaction.amount, currency)}</strong>
                </div>
                <div>
                  <span>{formatDateTime(transaction.timestamp)}</span>
                  <StatusBadge status={transaction.status} />
                </div>
              </div>
            );
          })}
        </div>

        {!transactions.isLoading && filtered.length === 0 && <EmptyState title="No transactions match the current filters" />}
      </Card>
    </div>
  );
}
