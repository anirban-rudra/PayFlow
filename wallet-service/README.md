# Wallet Service

Owns wallet balances, wallet holds, and wallet-side mutation records.

## Responsibilities

- Create wallets.
- Read wallet balance for the owner.
- Allow owner top-up.
- Support internal credit/debit/hold/capture/release operations.
- Enforce balance invariants.
- Use pessimistic locking for wallet mutation.
- Release expired holds through a scheduler.

## Gateway APIs

User-facing:

```text
GET  /api/v1/wallets/{userId}
POST /api/v1/wallets/{userId}/top-ups
```

Internal/service-only:

```text
POST /api/v1/wallets
POST /api/v1/wallets/credit
POST /api/v1/wallets/debit
POST /api/v1/wallets/hold
POST /api/v1/wallets/capture
POST /api/v1/wallets/release/{holdReference}
```

Normal users must not be given UI screens for direct credit, debit, capture, or release.

## Data Ownership

Flyway migrations create:

- `wallets`
- `wallet_holds`
- wallet-side `transactions`

Important invariants:

```text
balance >= 0
available_balance >= 0
available_balance <= balance
hold amount > 0
```

## Money Handling

Backend amount fields use `BigDecimal`. Database columns use `NUMERIC(19,2)`.

## Security Notes

- Owners can read and top up their own wallet.
- Internal mutation APIs require `X-Internal-Api-Key`.
- The service does not trust raw browser identity headers unless they came through the gateway.

## Local Commands

```bash
../mvnw test
../mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The service listens on `8088`.

## Related Docs

- [Root README](../Readme.md)
- [Design Document](../docs/DESIGN.md)
- [Transaction Service](../transaction-service/README.md)

