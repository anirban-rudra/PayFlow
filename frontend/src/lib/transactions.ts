import type { Transaction } from "../api/types";

export function isVisibleTransactionForUser(transaction: Transaction, userId?: number) {
  if (!userId) {
    return false;
  }

  if (transaction.senderId === userId) {
    return true;
  }

  return transaction.receiverId === userId && transaction.status === "SUCCESS";
}
