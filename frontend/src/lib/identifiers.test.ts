import { describe, expect, it } from "vitest";
import { formatCounterparty, formatRewardReference, formatTransactionReference } from "./identifiers";

describe("identifier formatting", () => {
  it("formats user-facing transaction and reward references", () => {
    expect(formatTransactionReference(2)).toBe("TXN-000002");
    expect(formatTransactionReference(2, "PF-TXN-ABC123")).toBe("PF-TXN-ABC123");
    expect(formatRewardReference(14)).toBe("RWD-000014");
  });

  it("uses PayTag when available and avoids internal user ID fallbacks", () => {
    expect(formatCounterparty("@receiver")).toBe("@receiver");
    expect(formatCounterparty()).toBe("PayFlow user");
    expect(formatCounterparty("")).toBe("PayFlow user");
  });
});
