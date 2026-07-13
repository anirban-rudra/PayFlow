import { request } from "./http";
import type { TopUpWalletRequest, WalletResponse } from "./types";

export function getWallet(userId: number) {
  return request<WalletResponse>(`/api/v1/wallets/${userId}`);
}

export function topUpWallet(userId: number, payload: TopUpWalletRequest) {
  return request<WalletResponse>(`/api/v1/wallets/${userId}/top-ups`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}
