export function requireTransactionWithinLimit(amount: string): void {
  const max = readMaxTransactionValue();
  const normalized = Number(amount);

  if (!Number.isFinite(normalized) || normalized < 0) {
    throw new Error("Invalid transaction amount");
  }
  if (normalized > max) {
    throw new Error(`Transaction amount exceeds MAX_TRANSACTION_VALUE_ETH (${max})`);
  }
}

export function readMaxTransactionValue(): number {
  const raw = process.env.MAX_TRANSACTION_VALUE_ETH;
  if (!raw) {
    throw new Error("MAX_TRANSACTION_VALUE_ETH is not configured");
  }
  const parsed = Number(raw);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error("MAX_TRANSACTION_VALUE_ETH must be a positive number");
  }
  return parsed;
}
