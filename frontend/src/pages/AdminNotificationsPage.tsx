import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { ApiError } from "../api/http";
import { Button, Card, ErrorBanner, FieldError, PageHeader } from "../components/ui";
import { useSendNotification } from "../hooks/usePayflow";
import { adminNotificationSchema, type AdminNotificationFormValues } from "../lib/validation";

export function AdminNotificationsPage() {
  const sendNotification = useSendNotification();
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const {
    register,
    clearErrors,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting }
  } = useForm<AdminNotificationFormValues>({
    resolver: zodResolver(adminNotificationSchema),
    reValidateMode: "onSubmit",
    shouldFocusError: false,
    defaultValues: {
      userId: "",
      message: ""
    }
  });

  const clearFeedback = (field: keyof AdminNotificationFormValues) => {
    setError("");
    setSuccess("");
    clearErrors(field);
  };
  const userIdField = register("userId", { onChange: () => clearFeedback("userId") });
  const messageField = register("message", { onChange: () => clearFeedback("message") });

  const submit = async (values: AdminNotificationFormValues) => {
    setError("");
    setSuccess("");

    try {
      await sendNotification.mutateAsync({ userId: Number(values.userId), message: values.message });
      setSuccess("Notification sent");
      reset();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not send notification");
    }
  };

  return (
    <div className="page-stack narrow-page">
      <PageHeader title="Notify ops" />
      <Card>
        <form className="form-stack" onSubmit={handleSubmit(submit)}>
          <ErrorBanner message={error} />
          {success && <div className="success-banner">{success}</div>}
          <label>
            <span>User ID</span>
            <input inputMode="numeric" {...userIdField} onFocus={() => clearFeedback("userId")} />
            <FieldError message={errors.userId?.message} />
          </label>
          <label>
            <span>Message</span>
            <textarea rows={5} {...messageField} onFocus={() => clearFeedback("message")} />
            <FieldError message={errors.message?.message} />
          </label>
          <Button loading={isSubmitting || sendNotification.isPending} type="submit">
            Send notification
          </Button>
        </form>
      </Card>
    </div>
  );
}
