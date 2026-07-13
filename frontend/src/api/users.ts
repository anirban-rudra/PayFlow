import { request } from "./http";
import type { TransferTargetResponse, UserResponse } from "./types";

export function getCurrentUser(userId: number) {
  return request<UserResponse>(`/api/users/${userId}`);
}

export function getUsers() {
  return request<UserResponse[]>("/api/users/all");
}

export function resolveTransferTarget(payTag: string) {
  return request<TransferTargetResponse>(`/api/users/resolve-transfer-target?payTag=${encodeURIComponent(payTag)}`);
}
