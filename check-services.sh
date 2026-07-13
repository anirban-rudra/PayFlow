#!/usr/bin/env bash
set -euo pipefail

echo "Checking PayFlow services..."

services=(
  "payflow-postgres-user"
  "payflow-postgres-wallet"
  "payflow-postgres-transaction"
  "payflow-postgres-reward"
  "payflow-postgres-notification"
  "payflow-zookeeper"
  "payflow-kafka"
  "payflow-redis"
  "payflow-user-service"
  "payflow-wallet-service"
  "payflow-transaction-service"
  "payflow-reward-service"
  "payflow-notification-service"
  "payflow-api-gateway"
)

running_services="$(docker ps --format '{{.Names}}\t{{.Status}}')"

for service in "${services[@]}"; do
    if grep -Fq "$service" <<< "$running_services"; then
        status=$(awk -F '\t' -v service="$service" '$1 == service {print $2}' <<< "$running_services")
        echo "[ok] $service: $status"
    else
        echo "[missing] $service: NOT RUNNING"
    fi
done

echo ""
echo "Frontend: http://localhost:3000"
echo "API Gateway: http://localhost:8080"
if grep -Fq "payflow-pgadmin" <<< "$running_services"; then
    echo "pgAdmin: http://localhost:5050"
fi
