import { request } from "./http";
import type { CreateTransactionRequest, Transaction, TransactionResponse } from "./types";

export function createTransaction(payload: CreateTransactionRequest, idempotencyKey: string) {
  return request<TransactionResponse>("/api/transactions/create", {
    method: "POST",
    headers: {
      "Idempotency-Key": idempotencyKey
    },
    body: JSON.stringify(payload)
  });
}

export function getTransactions(userId: number) {
  return request<Transaction[]>(`/api/transactions/user/${userId}`);
}

export function getTransaction(transactionId: number) {
  return request<Transaction>(`/api/transactions/${transactionId}`);
}
