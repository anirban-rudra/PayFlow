import { Component, type ErrorInfo, type ReactNode } from "react";
import { AlertTriangle } from "lucide-react";
import { Button } from "./ui";

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("Unhandled frontend error", error, info);
  }

  render() {
    if (this.state.hasError) {
      return (
        <main className="fatal-shell">
          <section className="fatal-panel">
            <AlertTriangle size={28} />
            <h1>Something went wrong</h1>
            <p>The dashboard could not render this view. Reload the app and try again.</p>
            <Button type="button" onClick={() => window.location.reload()}>
              Reload
            </Button>
          </section>
        </main>
      );
    }

    return this.props.children;
  }
}
