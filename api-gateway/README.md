# API Gateway

Spring Cloud Gateway entry point for all browser-facing backend traffic.

## Responsibilities

- Route `/auth/**` and `/api/**` requests to backend services.
- Validate JWTs for protected routes.
- Strip untrusted client-supplied identity/internal headers.
- Inject trusted headers:
  - `X-User-Id`
  - `X-User-Email`
  - `X-User-Role`
  - `X-Gateway-Request-Key`
- Apply Redis-backed rate limiting to transaction, reward, and notification routes.
- Configure CORS for the frontend origin.
- Expose health and Prometheus actuator endpoints.

## Routes

| Route | Target |
| --- | --- |
| `/auth/**` | `user-service:8081` |
| `/api/users/**` | `user-service:8081` |
| `/api/v1/wallets/**` | `wallet-service:8088` |
| `/api/wallets/**` | legacy rewrite to `/api/v1/wallets/**` |
| `/api/transactions/**` | `transaction-service:8082` |
| `/api/rewards/**` | `reward-service:8083` |
| `/api/notifications/**` | `notification-service:8084` |

## Security Notes

The gateway is the trust boundary between the browser and services. It removes any incoming user/internal headers before adding trusted values from the JWT. Services must still validate ownership/admin/internal access because defense in depth is required.

Required environment:

```text
JWT_SECRET
GATEWAY_REQUEST_KEY
FRONTEND_ALLOWED_ORIGINS
SPRING_DATA_REDIS_HOST
SPRING_DATA_REDIS_PORT
```

## Local Commands

```bash
../mvnw test
../mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The service listens on `8080`.

## Related Docs

- [Root README](../Readme.md)
- [Design Document](../docs/DESIGN.md)

