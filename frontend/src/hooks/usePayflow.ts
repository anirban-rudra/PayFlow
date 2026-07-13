import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getNotifications, markNotificationsRead, sendNotification } from "../api/notifications";
import { getAllRewards, getRewards } from "../api/rewards";
import { createTransaction, getTransactions } from "../api/transactions";
import { getCurrentUser, getUsers } from "../api/users";
import { getWallet, topUpWallet } from "../api/wallet";
import type { CreateTransactionRequest, SendNotificationRequest, TopUpWalletRequest } from "../api/types";

export function useCurrentUser(userId?: number) {
  return useQuery({
    queryKey: ["user", userId],
    queryFn: () => getCurrentUser(userId!),
    enabled: Boolean(userId)
  });
}

export function useWallet(userId?: number) {
  return useQuery({
    queryKey: ["wallet", userId],
    queryFn: () => getWallet(userId!),
    enabled: Boolean(userId)
  });
}

export function useTransactions(userId?: number) {
  return useQuery({
    queryKey: ["transactions", userId],
    queryFn: () => getTransactions(userId!),
    enabled: Boolean(userId)
  });
}

export function useRewards(userId?: number) {
  return useQuery({
    queryKey: ["rewards", userId],
    queryFn: () => getRewards(userId!),
    enabled: Boolean(userId)
  });
}

export function useNotifications(userId?: number) {
  return useQuery({
    queryKey: ["notifications", userId],
    queryFn: () => getNotifications(userId!),
    enabled: Boolean(userId)
  });
}

export function useMarkNotificationsRead(userId?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => markNotificationsRead(userId!),
    onSuccess: () => {
      if (userId) {
        void queryClient.invalidateQueries({ queryKey: ["notifications", userId] });
      }
    }
  });
}

export function useSendMoney(userId?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ payload, idempotencyKey }: { payload: CreateTransactionRequest; idempotencyKey: string }) =>
      createTransaction(payload, idempotencyKey),
    onSuccess: () => {
      if (userId) {
        void queryClient.invalidateQueries({ queryKey: ["wallet", userId] });
        void queryClient.invalidateQueries({ queryKey: ["transactions", userId] });
        void queryClient.invalidateQueries({ queryKey: ["rewards", userId] });
        void queryClient.invalidateQueries({ queryKey: ["notifications", userId] });
      }
    }
  });
}

export function useTopUpWallet(userId?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: TopUpWalletRequest) => topUpWallet(userId!, payload),
    onSuccess: () => {
      if (userId) {
        void queryClient.invalidateQueries({ queryKey: ["wallet", userId] });
      }
    }
  });
}

export function useAdminUsers(enabled: boolean) {
  return useQuery({
    queryKey: ["admin", "users"],
    queryFn: getUsers,
    enabled
  });
}

export function useAdminRewards(enabled: boolean) {
  return useQuery({
    queryKey: ["admin", "rewards"],
    queryFn: getAllRewards,
    enabled
  });
}

export function useSendNotification() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: SendNotificationRequest) => sendNotification(payload),
    onSuccess: (_notification, variables) => {
      void queryClient.invalidateQueries({ queryKey: ["notifications", variables.userId] });
    }
  });
}
