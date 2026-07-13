import { AtSign, BadgeCheck, CreditCard, UserRound } from "lucide-react";
import { Card, ErrorBanner, PageHeader } from "../components/ui";
import { useCurrentUser, useWallet } from "../hooks/usePayflow";
import { formatMoney } from "../lib/money";
import { useAuth } from "../state/auth";

export function ProfilePage() {
  const { user } = useAuth();
  const currentUser = useCurrentUser(user?.userId);
  const wallet = useWallet(user?.userId);
  const currency = wallet.data?.currency ?? "INR";
  const displayName = currentUser.data?.name ?? "PayFlow user";
  const payTag = currentUser.data?.payTag ?? "--";
  const accountType = user?.role === "ROLE_ADMIN" ? "Admin" : "Standard";

  return (
    <div className="page-stack narrow-page">
      <PageHeader title="Account" />
      <ErrorBanner message={currentUser.error ? "Could not load profile details." : undefined} />

      <Card className="profile-card">
        <div className="profile-block">
          <div className="avatar">{displayName.slice(0, 1).toUpperCase() ?? user?.email.slice(0, 1).toUpperCase()}</div>
          <div>
            <h2>{displayName}</h2>
            <span>{payTag}</span>
          </div>
          <span className="role-chip profile-role">{accountType}</span>
        </div>

        <div className="profile-stats">
          <div className="profile-metric">
            <span>Available to send</span>
            <strong>{wallet.data ? formatMoney(wallet.data.availableBalance, currency) : "--"}</strong>
          </div>
          <div className="profile-metric">
            <span>Wallet balance</span>
            <strong>{wallet.data ? formatMoney(wallet.data.balance, currency) : "--"}</strong>
          </div>
        </div>
      </Card>

      <Card>
        <div className="card-heading">
          <div>
            <h2>Account details</h2>
            <p>Core account and wallet settings</p>
          </div>
        </div>
        <div className="definition-list">
          <div>
            <span>
              <UserRound size={18} />
              Name
            </span>
            <strong>{displayName}</strong>
          </div>
          <div>
            <span>
              <AtSign size={18} />
              PayTag
            </span>
            <strong>{payTag}</strong>
          </div>
          <div>
            <span>
              <BadgeCheck size={18} />
              Account type
            </span>
            <strong>{accountType}</strong>
          </div>
          <div>
            <span>
              <CreditCard size={18} />
              Wallet currency
            </span>
            <strong>{currency}</strong>
          </div>
        </div>
      </Card>
    </div>
  );
}
