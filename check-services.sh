#!/bin/bash
echo "🔍 Checking PayPal Clone Services..."

services=(
  "postgres-user"
  "postgres-wallet"
  "postgres-transaction"
  "postgres-reward"
  "postgres-notification"
  "zookeeper"
  "kafka"
  "redis"
  "user-service"
  "wallet-service"
  "transaction-service"
  "reward-service"
  "notification-service"
  "api-gateway"
)

for service in "${services[@]}"; do
    if docker ps --format "table {{.Names}}\t{{.Status}}" | grep -q "$service"; then
        status=$(docker ps --format "table {{.Names}}\t{{.Status}}" | grep "$service" | awk '{print $2}')
        echo "✅ $service: $status"
    else
        echo "❌ $service: NOT RUNNING"
    fi
done

echo ""
echo "🌐 API Gateway: http://localhost:8080"
echo "📊 pgAdmin: http://localhost:5050 (admin@admin.com/admin)"