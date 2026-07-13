export type Role = "ROLE_USER" | "ROLE_ADMIN";

export interface AuthUser {
  userId: number;
  email: string;
  role: Role;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  name: string;
  email: string;
  payTag: string;
  password: string;
}

export interface JwtResponse {
  token: string;
}

export interface MessageResponse {
  message: string;
  userId?: number;
}

export interface UserResponse {
  id: number;
  name: string;
  email: string;
  payTag: string;
  role: Role;
}

export interface TransferTargetResponse {
  displayName: string;
  payTag: string;
}

export interface WalletResponse {
  id: number;
  userId: number;
  currency: string;
  balance: string | number;
  availableBalance: string | number;
}

export interface TopUpWalletRequest {
  amount: string;
  currency?: string;
}

export interface Transaction {
  id: number;
  publicReference?: string;
  holdReference?: string;
  failureReason?: string;
  completedAt?: string;
  senderId: number;
  receiverId: number;
  senderPayTag?: string;
  receiverPayTag?: string;
  amount: string | number;
  timestamp: string;
  status:
    | "CREATED"
    | "HOLD_PLACED"
    | "CAPTURED"
    | "CREDITED"
    | "SUCCESS"
    | "REFUND_PENDING"
    | "REFUNDED"
    | "MANUAL_REVIEW"
    | "FAILED"
    | string;
  message?: string;
}

export interface TransactionResponse extends Transaction {
  message: string;
}

export interface CreateTransactionRequest {
  receiverPayTag: string;
  amount: string;
}

export interface Reward {
  id: number;
  userId: number;
  transactionId: number;
  points: string | number;
  sentAt: string;
}

export interface Notification {
  id?: number;
  userId: number;
  message: string;
  sentAt?: string;
  readAt?: string | null;
  read?: boolean;
}

export interface SendNotificationRequest {
  userId: number;
  message: string;
}

export interface ApiProblem {
  message: string;
  status?: number;
}
