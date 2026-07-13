export function formatTransactionReference(id: number, publicReference?: string) {
  return publicReference?.trim() || `TXN-${String(id).padStart(6, "0")}`;
}

export function formatRewardReference(id: number) {
  return `RWD-${String(id).padStart(6, "0")}`;
}

export function formatCounterparty(payTag?: string) {
  return payTag?.trim() || "PayFlow user";
}
