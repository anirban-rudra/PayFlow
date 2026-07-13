# PayFlow Load Tests

k6 smoke and load-test scripts for the public API Gateway.

## Purpose

The load tests exercise realistic public flows:

- create users
- top up sender wallet
- send money by PayTag
- fetch transaction history

They intentionally call the gateway only. Internal service endpoints must not be targeted by load tests that model user behavior.

## Prerequisites

- PayFlow running locally or in staging.
- k6 installed.
- A disposable environment and test data.

Start local stack:

```bash
./start-payflow.sh --docker-app
```

Run the wallet transfer smoke test:

```bash
k6 run -e BASE_URL=http://localhost:8080 load-tests/k6/payflow-wallet-transfer.js
```

## Environment Variables

| Variable | Default | Purpose |
| --- | --- | --- |
| `BASE_URL` | `http://localhost:8080` | API Gateway URL |
| `TEST_PASSWORD` | script default | Password for generated users |

## Guidelines

- Do not run against production without explicit approval.
- Use isolated test accounts and wallets.
- Watch gateway, transaction-service, wallet-service, Kafka, and Postgres metrics.
- Check transaction failures, outbox backlog, and consumer lag after each run.
- Reset or archive test data between long runs.

## Useful Follow-Up Scenarios

- Concurrent transfers from one sender.
- Insufficient balance attempts.
- Repeated idempotency key submissions.
- Kafka unavailable during successful transfers.
- Reward/notification consumer lag.
- Notification mark-read load.
- Transaction history pagination once pagination is added.

## Related Docs

- [Root README](../Readme.md)
- [Design Document](../docs/DESIGN.md)
- [Deployment README](../deployment/README.md)
