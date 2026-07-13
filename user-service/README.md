# User Service

Owns users, authentication, roles, and PayTags.

## Responsibilities

- Register users.
- Normalize and validate PayTags.
- Enforce unique email and PayTag.
- Hash and store user passwords.
- Issue JWTs on login.
- Create a wallet through wallet-service during signup.
- Resolve PayTags for transfer UX and internal transaction flow.
- Provide admin user listing.

## Public Gateway APIs

```text
POST /auth/signup
POST /auth/login
GET  /api/users/{id}
GET  /api/users/all                         ROLE_ADMIN
GET  /api/users/resolve-transfer-target?payTag=@receiver
```

## Internal APIs

```text
GET /api/users/internal/resolve-pay-tag?payTag=@receiver
GET /api/users/internal/{id}
```

Internal resolution is used by transaction-service to avoid trusting client-submitted user IDs.

## Data Ownership

Flyway migrations create and evolve:

- `app_user`
- unique email index
- unique PayTag index

PayTags are stored normalized in lowercase and must start with `@`.

## Service Dependencies

- PostgreSQL `userdb`
- Wallet service for signup wallet creation

## Security Notes

- `/auth/**` is public.
- User read access is owner or admin.
- User listing requires `ROLE_ADMIN`.
- Internal wallet creation uses `INTERNAL_API_KEY`.
- JWT secret must be configured from the environment.

## Local Commands

```bash
../mvnw test
../mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The service listens on `8081`.

## Related Docs

- [Root README](../Readme.md)
- [Design Document](../docs/DESIGN.md)
- [Wallet Service](../wallet-service/README.md)

