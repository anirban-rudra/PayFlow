import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { login } from "../api/auth";
import { ApiError } from "../api/http";
import { Button, ErrorBanner, FieldError } from "../components/ui";
import { loginSchema, type LoginFormValues } from "../lib/validation";
import { useAuth } from "../state/auth";

export function LoginPage() {
  const { isAuthenticated, setSession } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState("");
  const {
    register,
    clearErrors,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    reValidateMode: "onSubmit",
    shouldFocusError: false,
    defaultValues: {
      email: "",
      password: ""
    }
  });

  const emailField = register("email", {
    onChange: () => {
      setError("");
      clearErrors("email");
    }
  });
  const passwordField = register("password", {
    onChange: () => {
      setError("");
      clearErrors("password");
    }
  });

  if (isAuthenticated) {
    return <Navigate to="/app/dashboard" replace />;
  }

  const submit = async (values: LoginFormValues) => {
    setError("");

    try {
      const response = await login(values);
      setSession(response.token);
      const next = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? "/app/dashboard";
      navigate(next, { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Login failed");
    }
  };

  return (
    <main className="auth-shell">
      <section className="auth-panel">
        <div className="auth-brand">
          <div className="brand-mark">P</div>
          <span>PayFlow</span>
        </div>
        <h1>Sign in</h1>
        <form className="form-stack" onSubmit={handleSubmit(submit)}>
          <ErrorBanner message={error} />
          <label>
            <span>Email</span>
            <input type="email" autoComplete="email" {...emailField} onFocus={() => clearErrors("email")} />
            <FieldError message={errors.email?.message} />
          </label>
          <label>
            <span>Password</span>
            <input type="password" autoComplete="current-password" {...passwordField} onFocus={() => clearErrors("password")} />
            <FieldError message={errors.password?.message} />
          </label>
          <Button loading={isSubmitting} type="submit">
            Sign in
          </Button>
        </form>
        <p className="auth-switch">
          New to PayFlow? <Link to="/auth/signup">Create account</Link>
        </p>
      </section>
    </main>
  );
}
