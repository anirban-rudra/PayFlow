# PayFlow Deployment

Deployment scaffolding and release guidance for PayFlow.

## Local Production-Like Shape

The Docker Compose setup mirrors the intended production topology:

- Frontend is served by Nginx.
- API Gateway is the public backend entry point.
- Service containers communicate on an internal network.
- Each service owns a separate PostgreSQL database.
- Kafka handles asynchronous reward and notification events.
- Redis supports gateway rate limiting.
- Secrets are supplied through environment variables.
- Actuator Prometheus endpoints can be scraped by the optional observability stack.

## Start Locally

```bash
./start-payflow.sh --docker-app
```

With pgAdmin:

```bash
./start-payflow.sh --docker-app --with-pgadmin
```

With observability:

```bash
./start-payflow.sh --docker-app --with-observability
```

Open:

- Frontend: http://localhost:3000
- API Gateway: http://localhost:8080
- pgAdmin: http://localhost:5050
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001

## Required Environment

Production-like deployments should provide:

```text
POSTGRES_USER
POSTGRES_PASSWORD
JWT_SECRET
INTERNAL_API_KEY
GATEWAY_REQUEST_KEY
PGADMIN_DEFAULT_PASSWORD
GRAFANA_ADMIN_PASSWORD
FRONTEND_ALLOWED_ORIGINS
REWARD_POINTS_RATE
REWARD_MINIMUM_AMOUNT
KAFKA_RETRY_INTERVAL_MS
KAFKA_RETRY_MAX_ATTEMPTS
```

Use a secret manager in real deployments. Do not bake secrets into images.

## Release Contract

A production deployment should follow this sequence:

1. Build immutable backend and frontend images from a tested commit.
2. Scan images and publish SBOMs.
3. Push images to the container registry.
4. Run database migrations in a controlled migration job.
5. Roll out stateless services.
6. Verify readiness probes.
7. Run smoke tests against the gateway.
8. Run a bounded k6 smoke load test.
9. Check dashboards and logs.
10. Enable traffic.

## Migration Rules

- Migrations live in each service under `src/main/resources/db/migration`.
- Hibernate DDL is `validate`.
- Prefer backward-compatible migrations:
  - add nullable columns first
  - backfill
  - enforce not-null/unique constraints in a later release when needed
- Avoid destructive migrations without backup and rollback planning.

## CI/CD

GitHub Actions currently validates:

- Backend Maven tests from the root reactor.
- Frontend typecheck, unit tests, and production build.

Recommended next pipeline stages:

- image build and scan
- migration dry-run
- staging deployment
- gateway smoke test
- k6 smoke test
- production promotion gate

## Runtime Options

Suitable targets:

- Docker host for local/demo deployment.
- AWS ECS/Fargate.
- Kubernetes.
- VM-based Docker Compose for demos only.

For Kubernetes, add:

- Helm or Kustomize manifests.
- per-service Deployments and Services.
- one Secret per secret group.
- migration Jobs.
- HPA rules.
- NetworkPolicies.
- PodDisruptionBudgets.

## Related Docs

- [Root README](../Readme.md)
- [Design Document](../docs/DESIGN.md)
- [Observability README](../observability/README.md)
- [Load Tests README](../load-tests/README.md)

