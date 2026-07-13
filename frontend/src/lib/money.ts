import Decimal from "decimal.js";

export function parseMoney(value: string): Decimal {
  const trimmed = value.trim();
  if (!/^\d+(\.\d{1,2})?$/.test(trimmed)) {
    throw new Error("Enter a valid amount");
  }

  const amount = new Decimal(trimmed);
  if (amount.lessThanOrEqualTo(0)) {
    throw new Error("Amount must be greater than zero");
  }
  return amount.toDecimalPlaces(2);
}

export function formatMoney(value: string | number | Decimal, currency = "INR") {
  const amount = new Decimal(value).toDecimalPlaces(2);
  const symbol = new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency,
    currencyDisplay: "narrowSymbol",
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  })
    .format(0)
    .replace(/[\d\s,.]/g, "");
  return `${symbol || currency} ${groupIndianAmount(amount.toFixed(2))}`;
}

export function formatPoints(value: string | number | Decimal) {
  return new Decimal(value).toDecimalPlaces(4).toFixed();
}

function groupIndianAmount(value: string) {
  const sign = value.startsWith("-") ? "-" : "";
  const unsigned = sign ? value.slice(1) : value;
  const [whole, fraction = "00"] = unsigned.split(".");

  if (whole.length <= 3) {
    return `${sign}${whole}.${fraction}`;
  }

  const lastThree = whole.slice(-3);
  const leading = whole.slice(0, -3).replace(/\B(?=(\d{2})+(?!\d))/g, ",");
  return `${sign}${leading},${lastThree}.${fraction}`;
}
