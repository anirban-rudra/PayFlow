import { Card, EmptyState, ErrorBanner, PageHeader, StatCard } from "../components/ui";
import { useRewards } from "../hooks/usePayflow";
import { formatRewardReference, formatTransactionReference } from "../lib/identifiers";
import { formatPoints } from "../lib/money";
import { sumRewardPoints } from "../lib/rewards";
import { formatDateTime } from "../lib/time";
import { useAuth } from "../state/auth";

export function RewardsPage() {
  const { user } = useAuth();
  const rewards = useRewards(user?.userId);
  const total = sumRewardPoints(rewards.data);

  return (
    <div className="page-stack">
      <PageHeader title="Rewards" />
      <ErrorBanner message={rewards.error ? "Could not load rewards." : undefined} />
      <div className="stat-grid compact-stats">
        <StatCard label="Total points" value={formatPoints(total)} tone="success" />
        <StatCard label="Reward events" value={String(rewards.data?.length ?? 0)} />
        <StatCard label="Earn rate" value="5%" />
        <StatCard label="Minimum transfer" value="INR 5.00" />
      </div>
      <Card>
        <div className="card-heading">
          <div>
            <h2>Reward history</h2>
            <p>Points are issued to the sender after successful eligible transfers</p>
          </div>
        </div>
        <div className="desktop-table">
          <table>
            <thead>
              <tr>
                <th>Reward</th>
                <th>Transaction</th>
                <th>Points</th>
                <th>Issued</th>
              </tr>
            </thead>
            <tbody>
              {[...(rewards.data ?? [])]
                .sort((a, b) => new Date(b.sentAt).getTime() - new Date(a.sentAt).getTime())
                .map((reward) => (
                  <tr key={reward.id}>
                    <td>{formatRewardReference(reward.id)}</td>
                    <td>{formatTransactionReference(reward.transactionId)}</td>
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
                <strong>{formatPoints(reward.points)} points</strong>
                <span>{formatRewardReference(reward.id)}</span>
              </div>
              <div>
                <span>{formatTransactionReference(reward.transactionId)}</span>
                <span>{formatDateTime(reward.sentAt)}</span>
              </div>
            </div>
          ))}
        </div>
        {!rewards.isLoading && (rewards.data ?? []).length === 0 && <EmptyState title="No rewards issued yet" />}
      </Card>
    </div>
  );
}
