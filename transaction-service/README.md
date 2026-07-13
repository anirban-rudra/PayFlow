# Transaction Service

Saga orchestrator for wallet-to-wallet transfers.

## Responsibilities

- Accept transfer requests by receiver PayTag.
- Validate idempotency keys.
- Resolve receiver and sender PayTags through user-service.
- Coordinate wallet hold, capture, receiver credit, and compensation.
- Persist transaction state transitions.
- Store display PayTags in transaction history.
- Save outbox events after successful transfers.
- Reconcile stale or incomplete transaction states.

## Gateway APIs

```text
POST /api/transactions/create
GET  /api/transactions/{id}
GET  /api/transactions/user/{userId}
GET  /api/transactions/reconciliation       ROLE_ADMIN
POST /api/transactions/reconciliation/{id}  ROLE_ADMIN
POST /api/transactions/reconciliation/stale ROLE_ADMIN
```

## Transfer State Flow

```text
CREATED -> HOLD_PLACED -> CAPTURED -> CREDITED -> SUCCESS
```

Failure states:

```text
FAILED
REFUND_PENDING
REFUNDED
MANUAL_REVIEW
```

## Data Ownership

Flyway migrations create and evolve:

- `transactions`
- `transaction_outbox_events`

Important constraints:

- positive amount
- sender and receiver must differ
- unique `(sender_id, idempotency_key)` when idempotency key is supplied
- unique public transaction reference

## Outbox

Successful transfers create an outbox row for the transaction event. The outbox publisher sends the event to Kafka and retries if Kafka is unavailable.

## History Product Rules

- Sender sees successful and failed outgoing attempts.
- Receiver sees only successful incoming transfers.
- Incoming rows display `senderPayTag`.
- Outgoing rows display `receiverPayTag`.

## Security Notes

- Transaction service expects the gateway request key.
- User identity comes from trusted gateway headers.
- Wallet mutation calls use internal API key.

## Local Commands

```bash
../mvnw test
../mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The service listens on `8082`.

## Related Docs

- [Root README](../Readme.md)
- [Design Document](../docs/DESIGN.md)
- [Wallet Service](../wallet-service/README.md)
- [User Service](../user-service/README.md)

