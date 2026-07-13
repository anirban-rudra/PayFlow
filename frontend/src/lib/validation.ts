import { z } from "zod";

const amountMessage = "Use a positive amount with up to 2 decimals";
const payTagRegex = /^@[A-Za-z0-9][A-Za-z0-9._-]{2,29}$/;
const payTagMessage = "PayTag must start with @ and use 3-30 letters, numbers, dots, underscores, or hyphens";

export const loginSchema = z.object({
  email: z.string().trim().email("Enter a valid email"),
  password: z.string().min(1, "Password is required")
});

export const signupSchema = z
  .object({
    name: z.string().trim().min(1, "Name is required").max(120, "Name is too long"),
    email: z.string().trim().email("Enter a valid email"),
    payTag: z.string().trim().regex(payTagRegex, payTagMessage),
    password: z.string().min(8, "Password must be at least 8 characters"),
    confirmPassword: z.string().min(1, "Confirm your password")
  })
  .refine((value) => value.password === value.confirmPassword, {
    path: ["confirmPassword"],
    message: "Passwords do not match"
  });

export const sendMoneySchema = z.object({
  receiverPayTag: z.string().trim().regex(payTagRegex, payTagMessage),
  amount: z.string().trim().regex(/^\d+(\.\d{1,2})?$/, amountMessage),
  note: z.string().trim().max(140, "Note must be 140 characters or less").optional()
});

export const adminNotificationSchema = z.object({
  userId: z
    .string()
    .trim()
    .regex(/^\d+$/, "Enter a valid user ID")
    .refine((value) => Number(value) > 0, "Enter a valid user ID"),
  message: z.string().trim().min(1, "Message is required").max(500, "Message is too long")
});

export type LoginFormValues = z.infer<typeof loginSchema>;
export type SignupFormValues = z.infer<typeof signupSchema>;
export type SendMoneyFormValues = z.infer<typeof sendMoneySchema>;
export type AdminNotificationFormValues = z.infer<typeof adminNotificationSchema>;
