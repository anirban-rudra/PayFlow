import { describe, expect, it } from "vitest";
import { loginSchema, sendMoneySchema, signupSchema } from "./validation";

describe("form validation", () => {
  it("validates login credentials", () => {
    expect(loginSchema.safeParse({ email: "bad", password: "" }).success).toBe(false);
    expect(loginSchema.safeParse({ email: "user@example.com", password: "secret" }).success).toBe(true);
  });

  it("requires matching signup passwords", () => {
    const result = signupSchema.safeParse({
      name: "User",
      email: "user@example.com",
      payTag: "@user",
      password: "password123",
      confirmPassword: "different"
    });

    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some((issue) => issue.path.includes("confirmPassword"))).toBe(true);
    }
  });

  it("validates transfer values without accepting float-like edge cases", () => {
    expect(sendMoneySchema.safeParse({ receiverPayTag: "@receiver", amount: "10.50", note: "" }).success).toBe(true);
    expect(sendMoneySchema.safeParse({ receiverPayTag: "@receiver", amount: "10.555", note: "" }).success).toBe(false);
    expect(sendMoneySchema.safeParse({ receiverPayTag: "receiver", amount: "10.50", note: "" }).success).toBe(false);
  });
});
