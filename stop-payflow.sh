#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STATE_DIR="$ROOT_DIR/.payflow"
PID_FILE="$STATE_DIR/pids"

STOP_DOCKER=true

usage() {
  cat <<'EOF'
Usage: ./stop-payflow.sh [options]

Stops PayFlow local processes started by ./start-payflow.sh and stops Docker dependencies.

Options:
  --keep-docker   Stop only Maven/npm processes and leave Docker dependencies running.
  -h, --help      Show this help.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --keep-docker)
      STOP_DOCKER=false
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
  shift
done

stop_pid_file_processes() {
  if [[ ! -f "$PID_FILE" ]]; then
    echo "No PID file found at $PID_FILE"
    return 0
  fi

  while IFS=: read -r name pid; do
    if [[ -z "${name:-}" || -z "${pid:-}" ]]; then
      continue
    fi

    if kill -0 "$pid" >/dev/null 2>&1; then
      echo "Stopping $name (pid $pid)..."
      kill "$pid" >/dev/null 2>&1 || true
    else
      echo "$name pid $pid is not running."
    fi
  done < "$PID_FILE"

  sleep 3

  while IFS=: read -r name pid; do
    if [[ -z "${name:-}" || -z "${pid:-}" ]]; then
      continue
    fi

    if kill -0 "$pid" >/dev/null 2>&1; then
      echo "Force stopping $name (pid $pid)..."
      kill -9 "$pid" >/dev/null 2>&1 || true
    fi
  done < "$PID_FILE"

  rm -f "$PID_FILE"
}

docker_compose_available() {
  command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1
}

stop_docker_dependencies() {
  local services=(
    grafana
    prometheus
    frontend
    api-gateway
    user-service
    wallet-service
    transaction-service
    reward-service
    notification-service
    pgadmin
    redis
    kafka
    zookeeper
    postgres-user
    postgres-wallet
    postgres-transaction
    postgres-reward
    postgres-notification
  )

  echo "Stopping Docker Compose services..."
  if docker_compose_available; then
    docker compose stop "${services[@]}" >/dev/null 2>&1 || true
  else
    echo "Docker Compose is not available; skipped Docker dependency shutdown."
  fi
}

stop_pid_file_processes

if [[ "$STOP_DOCKER" == "true" ]]; then
  stop_docker_dependencies
fi

echo "PayFlow stopped."
