import { SendMoneyForm } from "../components/SendMoneyForm";
import { Card, PageHeader } from "../components/ui";

export function SendMoneyPage() {
  return (
    <div className="page-stack narrow-page">
      <PageHeader title="Send money" />
      <Card>
        <SendMoneyForm />
      </Card>
    </div>
  );
}
