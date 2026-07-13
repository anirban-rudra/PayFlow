import { Card, EmptyState, ErrorBanner, PageHeader, StatCard } from "../components/ui";
import { useAdminRewards } from "../hooks/usePayflow";
import { formatPoints } from "../lib/money";
import { sumRewardPoints } from "../lib/rewards";
import { formatDateTime } from "../lib/time";
import { useAuth } from "../state/auth";

export function AdminRewardsPage() {
  const { user } = useAuth();
  const rewards = useAdminRewards(user?.role === "ROLE_ADMIN");
  const total = sumRewardPoints(rewards.data);

  return (
    <div className="page-stack">
      <PageHeader title="Reward ops" />
      <ErrorBanner message={rewards.error ? "Could not load reward operations." : undefined} />
      <div className="stat-grid compact-stats">
        <StatCard label="Issued points" value={formatPoints(total)} tone="success" />
        <StatCard label="Reward rows" value={String(rewards.data?.length ?? 0)} />
      </div>
      <Card>
        <div className="desktop-table">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>User</th>
                <th>Transaction</th>
                <th>Points</th>
                <th>Issued</th>
              </tr>
            </thead>
            <tbody>
              {(rewards.data ?? []).map((reward) => (
                <tr key={reward.id}>
                  <td>#{reward.id}</td>
                  <td>User #{reward.userId}</td>
                  <td>#{reward.transactionId}</td>
                  <td>{formatPoints(reward.points)}</td>
                  <td>{formatDateTime(reward.sentAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="mobile-list">
          {(rewards.data ?? []).map((reward) => (
            <div className="mobile-record" key={reward.id}>
              <div>
                <strong>User #{reward.userId}</strong>
                <span>Reward #{reward.id}</span>
              </div>
              <div>
                <span>Transaction #{reward.transactionId}</span>
                <strong>{formatPoints(reward.points)} pts</strong>
              </div>
              <div>
                <span>{formatDateTime(reward.sentAt)}</span>
              </div>
            </div>
          ))}
        </div>
        {!rewards.isLoading && (rewards.data ?? []).length === 0 && <EmptyState title="No rewards found" />}
      </Card>
    </div>
  );
}
