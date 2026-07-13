function positiveInteger(value: string | undefined, fallback: number) {
  if (!value) {
    return fallback;
  }

  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

export const config = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? "",
  requestTimeoutMs: positiveInteger(import.meta.env.VITE_API_TIMEOUT_MS, 15_000)
};
