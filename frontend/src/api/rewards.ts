import { request } from "./http";
import type { Reward } from "./types";

export function getRewards(userId: number) {
  return request<Reward[]>(`/api/rewards/user/${userId}`);
}

export function getAllRewards() {
  return request<Reward[]>("/api/rewards");
}

export function getRewardsByTransaction(transactionId: number) {
  return request<Reward>(`/api/rewards/transaction/${transactionId}`);
}
