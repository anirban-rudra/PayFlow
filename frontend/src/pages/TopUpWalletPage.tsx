import { ArrowLeftRight } from "lucide-react";
import { Link } from "react-router-dom";
import { LoadBalanceForm } from "../components/LoadBalanceForm";
import { Card, PageHeader } from "../components/ui";

export function TopUpWalletPage() {
  return (
    <div className="page-stack narrow-page">
      <PageHeader
        title="Top Up Wallet"
        actions={
          <Link className="button button-secondary" to="/app/send">
            <ArrowLeftRight size={17} />
            Send money
          </Link>
        }
      />

      <Card>
        <div className="card-heading">
          <div>
            <h2>Add money</h2>
            <p>Increase your available wallet balance</p>
          </div>
        </div>
        <LoadBalanceForm />
      </Card>
    </div>
  );
}
