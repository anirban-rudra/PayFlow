import Decimal from "decimal.js";
import { describe, expect, it } from "vitest";
import { formatMoney, formatPoints, parseMoney } from "./money";

describe("money utilities", () => {
  it("accepts positive values with up to two decimals", () => {
    expect(parseMoney("10").toFixed(2)).toBe("10.00");
    expect(parseMoney("10.5").toFixed(2)).toBe("10.50");
    expect(parseMoney("10.55").toFixed(2)).toBe("10.55");
  });

  it("rejects invalid financial amounts", () => {
    expect(() => parseMoney("0")).toThrow("Amount must be greater than zero");
    expect(() => parseMoney("-1")).toThrow("Enter a valid amount");
    expect(() => parseMoney("10.555")).toThrow("Enter a valid amount");
    expect(() => parseMoney("abc")).toThrow("Enter a valid amount");
  });

  it("formats large INR amounts without converting through native floats", () => {
    expect(formatMoney(new Decimal("12345678901234567890.12"), "INR")).toBe("₹ 1,23,45,67,89,01,23,45,67,890.12");
  });

  it("formats reward points to four decimals", () => {
    expect(formatPoints("1.234567")).toBe("1.2346");
  });
});
