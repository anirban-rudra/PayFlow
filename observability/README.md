# Observability

Optional local Prometheus and Grafana setup for PayFlow.

## Start

```bash
./start-payflow.sh --with-observability
```

Or directly:

```bash
docker compose -f docker-compose.yml -f docker-compose.observability.yml up -d prometheus grafana
```

## URLs

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001

## Metrics Sources

Spring services expose:

```text
/actuator/health
/actuator/prometheus
```

The frontend exposes:

```text
/healthz
```

## Recommended Dashboards

Add dashboards for:

- API latency and status codes.
- Transfer success/failure rate.
- Transaction state counts.
- Outbox backlog.
- Kafka consumer lag.
- Manual review count.
- Wallet hold release failures.
- JVM memory, CPU, and GC.

## Related Docs

- [Root README](../Readme.md)
- [Design Document](../docs/DESIGN.md)

