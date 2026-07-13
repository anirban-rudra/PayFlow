# Reward Service

Owns reward calculation, reward persistence, and reward read APIs.

## Responsibilities

- Consume successful transaction events from Kafka.
- Calculate reward points.
- Enforce minimum transaction amount for rewards.
- Store rewards idempotently by transaction ID.
- Expose user and admin reward views.

## Gateway APIs

```text
GET /api/rewards/user/{userId}
GET /api/rewards                            ROLE_ADMIN
GET /api/rewards/transaction/{transactionId}
```

## Data Ownership

Flyway migrations create:

- `reward`
- index on `user_id`
- unique index on `transaction_id`

The transaction unique index prevents duplicate rewards if Kafka redelivers the same transaction event.

## Configuration

```text
REWARD_POINTS_RATE
REWARD_MINIMUM_AMOUNT
KAFKA_RETRY_INTERVAL_MS
KAFKA_RETRY_MAX_ATTEMPTS
```

## Security Notes

- Users can read their own rewards.
- Admins can read all rewards.
- Rewards are not created through browser calls; they are generated from Kafka transaction events.

## Local Commands

```bash
../mvnw test
../mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The service listens on `8083`.

## Related Docs

- [Root README](../Readme.md)
- [Design Document](../docs/DESIGN.md)
