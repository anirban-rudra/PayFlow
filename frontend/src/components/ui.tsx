import { AlertCircle, Loader2, X } from "lucide-react";
import type { ButtonHTMLAttributes, ReactNode } from "react";

export function PageHeader({ title, actions }: { title: string; actions?: ReactNode }) {
  return (
    <div className="page-header">
      <h1>{title}</h1>
      {actions && <div className="page-actions">{actions}</div>}
    </div>
  );
}

export function Card({ children, className = "" }: { children: ReactNode; className?: string }) {
  return <section className={`card ${className}`}>{children}</section>;
}

export function StatCard({
  label,
  value,
  tone = "neutral"
}: {
  label: string;
  value: string;
  tone?: "neutral" | "success" | "warning";
}) {
  return (
    <Card className={`stat-card stat-${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </Card>
  );
}

export function StatusBadge({ status }: { status: string }) {
  const normalized = status.toLowerCase().replace(/[^a-z0-9-]/g, "-");
  return <span className={`status-badge status-${normalized}`}>{status}</span>;
}

export function Button({
  children,
  variant = "primary",
  loading,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "danger";
  loading?: boolean;
}) {
  return (
    <button className={`button button-${variant}`} disabled={props.disabled || loading} {...props}>
      {loading && <Loader2 className="spin" size={16} />}
      <span>{children}</span>
    </button>
  );
}

export function EmptyState({ title }: { title: string }) {
  return (
    <div className="empty-state">
      <AlertCircle size={22} />
      <span>{title}</span>
    </div>
  );
}

export function FieldError({ message }: { message?: string }) {
  if (!message) {
    return null;
  }
  return <div className="field-error">{message}</div>;
}

export function ErrorBanner({ message }: { message?: string }) {
  if (!message) {
    return null;
  }
  return <div className="error-banner">{message}</div>;
}

export function Modal({
  title,
  children,
  footer,
  onClose
}: {
  title: string;
  children: ReactNode;
  footer?: ReactNode;
  onClose: () => void;
}) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal-panel" role="dialog" aria-modal="true" aria-labelledby="modal-title" onMouseDown={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <h2 id="modal-title">{title}</h2>
          <button className="icon-button" type="button" onClick={onClose} aria-label="Close dialog">
            <X size={18} />
          </button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-footer">{footer}</div>}
      </section>
    </div>
  );
}
