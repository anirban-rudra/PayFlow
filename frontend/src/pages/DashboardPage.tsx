import { Link } from "react-router-dom";
import { ArrowDownLeft, ArrowUpRight, Bell, Expand, Gift } from "lucide-react";
import { LoadBalanceForm } from "../components/LoadBalanceForm";
import { SendMoneyForm } from "../components/SendMoneyForm";
import { Card, EmptyState, ErrorBanner, PageHeader, StatCard, StatusBadge } from "../components/ui";
import { useNotifications, useRewards, useTransactions, useWallet } from "../hooks/usePayflow";
import { formatCounterparty } from "../lib/identifiers";
import { formatMoney, formatPoints } from "../lib/money";
import { sumRewardPoints } from "../lib/rewards";
import { formatDateTime } from "../lib/time";
import { isVisibleTransactionForUser } from "../lib/transactions";
import { useAuth } from "../state/auth";

export function DashboardPage() {
  const { user } = useAuth();
  const wallet = useWallet(user?.userId);
  const transactions = useTransactions(user?.userId);
  const rewards = useRewards(user?.userId);
  const notifications = useNotifications(user?.userId);
  const currency = wallet.data?.currency ?? "INR";
  const unreadAlertCount = (notifications.data ?? []).filter((notification) => !notification.read && !notification.readAt).length;
  const recentTransactions = [...(transactions.data ?? [])]
    .filter((transaction) => isVisibleTransactionForUser(transaction, user?.userId))
    .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
    .slice(0, 5);
  const totalRewardPoints = sumRewardPoints(rewards.data);

  return (
    <div className="page-stack">
      <PageHeader title="Dashboard" />

      <ErrorBanner message={wallet.error ? "Wallet details are not available right now." : undefined} />

      <div className="stat-grid">
        <StatCard label="Available balance" value={wallet.data ? formatMoney(wallet.data.availableBalance, currency) : "--"} />
        <StatCard label="Ledger balance" value={wallet.data ? formatMoney(wallet.data.balance, currency) : "--"} />
        <StatCard label="Reward points" value={formatPoints(totalRewardPoints)} tone="success" />
        <StatCard label="Unread alerts" value={String(unreadAlertCount)} tone="warning" />
      </div>

      <div className="dashboard-grid">
        <Card>
          <div className="card-heading">
            <div>
              <h2>Add money</h2>
              <p>Top up your wallet</p>
            </div>
            <Link className="button button-secondary action-button icon-action" to="/app/top-up" aria-label="Open Top Up Wallet">
              <Expand size={15} />
            </Link>
          </div>
          <LoadBalanceForm />
        </Card>

        <Card className="quick-send-card">
          <div className="card-heading">
            <div>
              <h2>Quick send</h2>
              <p>Wallet-to-wallet transfer</p>
            </div>
            <Link className="button button-secondary action-button icon-action" to="/app/send" aria-label="Open Send Money">
              <Expand size={15} />
            </Link>
          </div>
          <SendMoneyForm compact />
        </Card>

        <Card>
          <div className="card-heading">
            <div>
              <h2>Recent transactions</h2>
              <p>Latest transfers</p>
            </div>
            <Link className="text-link" to="/app/transactions">
              View all
            </Link>
          </div>
          <div className="activity-list">
            {transactions.isLoading && <EmptyState title="Loading transactions" />}
            {!transactions.isLoading && recentTransactions.length === 0 && <EmptyState title="No transaction activity yet" />}
            {recentTransactions.map((transaction) => {
              const incoming = transaction.receiverId === user?.userId;
              const Icon = incoming ? ArrowDownLeft : ArrowUpRight;
              const counterparty = incoming ? formatCounterparty(transaction.senderPayTag) : formatCounterparty(transaction.receiverPayTag);
              return (
                <div className="activity-row" key={transaction.id}>
                  <div className={`activity-icon ${incoming ? "incoming" : "outgoing"}`}>
                    <Icon size={18} />
                  </div>
                  <div className="activity-main">
                    <strong>{incoming ? `From ${counterparty}` : `To ${counterparty}`}</strong>
                    <span>{formatDateTime(transaction.timestamp)}</span>
                  </div>
                  <div className="activity-meta">
                    <strong>{formatMoney(transaction.amount, currency)}</strong>
                    <StatusBadge status={transaction.status} />
                  </div>
                </div>
              );
            })}
          </div>
        </Card>

        <Card>
          <div className="card-heading compact">
            <div>
              <h2>Rewards</h2>
              <p>Points issued from successful transfers</p>
            </div>
            <Gift size={20} />
          </div>
          <div className="summary-line">
            <span>Events</span>
            <strong>{rewards.data?.length ?? 0}</strong>
          </div>
          <div className="summary-line">
            <span>Total points</span>
            <strong>{formatPoints(totalRewardPoints)}</strong>
          </div>
        </Card>

        <Card>
          <div className="card-heading compact">
            <div>
              <h2>Alerts</h2>
              <p>Recent account notifications</p>
            </div>
            <Bell size={20} />
          </div>
          <div className="mini-list">
            {(notifications.data ?? []).slice(0, 3).map((notification) => (
              <div key={`${notification.id ?? notification.message}-${notification.sentAt ?? ""}`}>
                <strong>{notification.message}</strong>
                <span>{formatDateTime(notification.sentAt)}</span>
              </div>
            ))}
            {(notifications.data ?? []).length === 0 && <EmptyState title="No alerts" />}
          </div>
        </Card>
      </div>
    </div>
  );
}
