# PayFlow Frontend

Responsive fintech dashboard for the PayFlow microservice backend.

The frontend is a product application, not a landing page. After login, users land directly on the dashboard and can manage wallet balance, transfers, transaction history, rewards, notifications, and profile details.

## Stack

- React
- TypeScript
- Vite
- React Router
- TanStack Query
- React Hook Form
- Zod
- `decimal.js`
- lucide-react icons
- Nginx production container

## Route Map

```text
/auth/login
/auth/signup

/app/dashboard
/app/top-up
/app/send
/app/transactions
/app/rewards
/app/notifications
/app/profile

/admin/users
/admin/rewards
/admin/notifications
```

Route rules:

- `/app/*` requires a valid JWT.
- `/admin/*` requires a valid JWT with `ROLE_ADMIN`.

## Source Structure

```text
src/api/http.ts              HTTP client, JWT attachment, timeout, unauthorized handling
src/api/*.ts                 Resource-specific API functions
src/hooks/usePayflow.ts      TanStack Query hooks and cache invalidation
src/state/auth.tsx           Auth/session provider
src/components               Shared layout, buttons, forms, modal, UI primitives
src/pages                    Route-level pages
src/lib                      Money, validation, identifier, reward, time helpers
src/test                     Test helpers
```

## Important UX Rules

- Use PayTag, not user ID, for sending money.
- Verify recipient PayTag before showing transfer confirmation.
- Do not show failed incoming transaction attempts in receiver history.
- Show sender PayTag for incoming transactions.
- Show receiver PayTag for outgoing transactions.
- Show a receipt only for successful transfers.
- Clear stale form errors on focus/edit; revalidate on submit.
- Count unread alerts from `readAt`, not total notifications.
- Restore the stored session before dashboard queries run.

## API Client

The browser talks only to the API Gateway.

In production Docker, Nginx serves the app and proxies:

```text
/auth -> api-gateway:8080
/api  -> api-gateway:8080
```

In local Vite development, the dev server proxies the same paths to `http://localhost:8080`.

## Money Handling

Do not use native floating point for financial decisions. The frontend uses `decimal.js` for:

- transfer amount parsing
- top-up amount parsing
- balance comparison
- reward-point formatting

The backend remains the source of truth for final validation.

## Configuration

Copy `.env.example` when local overrides are needed.

| Variable | Default | Purpose |
| --- | --- | --- |
| `VITE_API_BASE_URL` | empty | API origin. Leave empty when frontend and gateway share origin or proxying is used. |
| `VITE_API_TIMEOUT_MS` | `15000` | Client-side API request timeout in milliseconds. |

## Local Development

From `frontend`:

```bash
npm install
npm run dev
```

Open http://localhost:3000.

## Verification

```bash
npm run typecheck
npm run test
npm run build
npm run verify
```

`npm run verify` runs typecheck, tests, and production build.

## Production Build

```bash
npm ci
npm run build
```

Build output goes to `dist/`.

## Container

```bash
docker build -t payflow-frontend .
docker run --rm -p 3000:80 payflow-frontend
```

The image exposes:

```text
/healthz
```

## Extension Guidelines

- Add API calls under `src/api`.
- Add server-state hooks under `src/hooks/usePayflow.ts`.
- Keep shared UI primitives in `src/components`.
- Keep route-level composition in `src/pages`.
- Keep financial parsing/formatting in `src/lib/money.ts`.
- Add tests for every user-facing bug fix and critical flow.

## Related Docs

- [Root README](../Readme.md)
- [Design Document](../docs/DESIGN.md)

