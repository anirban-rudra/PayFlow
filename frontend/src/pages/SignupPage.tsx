import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { signup } from "../api/auth";
import { ApiError } from "../api/http";
import { Button, ErrorBanner, FieldError } from "../components/ui";
import { signupSchema, type SignupFormValues } from "../lib/validation";
import { useAuth } from "../state/auth";

export function SignupPage() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const {
    register,
    clearErrors,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<SignupFormValues>({
    resolver: zodResolver(signupSchema),
    reValidateMode: "onSubmit",
    shouldFocusError: false,
    defaultValues: {
      name: "",
      email: "",
      payTag: "",
      password: "",
      confirmPassword: ""
    }
  });

  const clearFeedback = (field: keyof SignupFormValues) => {
    setError("");
    setMessage("");
    clearErrors(field);
  };
  const nameField = register("name", { onChange: () => clearFeedback("name") });
  const emailField = register("email", { onChange: () => clearFeedback("email") });
  const payTagField = register("payTag", { onChange: () => clearFeedback("payTag") });
  const passwordField = register("password", { onChange: () => clearFeedback("password") });
  const confirmPasswordField = register("confirmPassword", { onChange: () => clearFeedback("confirmPassword") });

  if (isAuthenticated) {
    return <Navigate to="/app/dashboard" replace />;
  }

  const submit = async (values: SignupFormValues) => {
    setError("");
    setMessage("");

    try {
      const response = await signup({ name: values.name, email: values.email, payTag: values.payTag, password: values.password });
      setMessage(response.message);
      setTimeout(() => navigate("/auth/login"), 700);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Signup failed");
    }
  };

  return (
    <main className="auth-shell">
      <section className="auth-panel">
        <div className="auth-brand">
          <div className="brand-mark">P</div>
          <span>PayFlow</span>
        </div>
        <h1>Create account</h1>
        <form className="form-stack" onSubmit={handleSubmit(submit)}>
          <ErrorBanner message={error} />
          {message && <div className="success-banner">{message}</div>}
          <label>
            <span>Name</span>
            <input autoComplete="name" {...nameField} onFocus={() => clearFeedback("name")} />
            <FieldError message={errors.name?.message} />
          </label>
          <label>
            <span>Email</span>
            <input type="email" autoComplete="email" {...emailField} onFocus={() => clearFeedback("email")} />
            <FieldError message={errors.email?.message} />
          </label>
          <label>
            <span>PayTag</span>
            <input autoComplete="off" placeholder="@yourname" {...payTagField} onFocus={() => clearFeedback("payTag")} />
            <FieldError message={errors.payTag?.message} />
          </label>
          <label>
            <span>Password</span>
            <input type="password" autoComplete="new-password" {...passwordField} onFocus={() => clearFeedback("password")} />
            <FieldError message={errors.password?.message} />
          </label>
          <label>
            <span>Confirm password</span>
            <input type="password" autoComplete="new-password" {...confirmPasswordField} onFocus={() => clearFeedback("confirmPassword")} />
            <FieldError message={errors.confirmPassword?.message} />
          </label>
          <Button loading={isSubmitting} type="submit">
            Create account
          </Button>
        </form>
        <p className="auth-switch">
          Already registered? <Link to="/auth/login">Sign in</Link>
        </p>
      </section>
    </main>
  );
}
