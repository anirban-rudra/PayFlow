#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STATE_DIR="$ROOT_DIR/.payflow"
LOG_DIR="$STATE_DIR/logs"
PID_FILE="$STATE_DIR/pids"
LOCAL_ENV_FILE="$STATE_DIR/local.env"

DOCKER_APP=false
WITH_PGADMIN=false
SKIP_FRONTEND=false
WITH_OBSERVABILITY=false

usage() {
  cat <<'EOF'
Usage: ./start-payflow.sh [options]

Starts the PayFlow local full-stack app.

Default mode:
  - starts Docker infrastructure: Postgres, Kafka, Zookeeper, Redis
  - starts Spring Boot services with the local profile
  - starts the Vite frontend on http://localhost:3000

Options:
  --docker-app      Run the whole app with Docker Compose instead of local Maven/npm processes.
  --with-pgadmin    Also start pgAdmin at http://localhost:5050.
  --with-observability
                    Also start Prometheus at http://localhost:9090 and Grafana at http://localhost:3001.
  --skip-frontend   Start backend and dependencies only.
  -h, --help        Show this help.

Use ./stop-payflow.sh to stop local Maven/npm processes and Docker dependencies.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --docker-app)
      DOCKER_APP=true
      ;;
    --with-pgadmin)
      WITH_PGADMIN=true
      ;;
    --with-observability)
      WITH_OBSERVABILITY=true
      ;;
    --skip-frontend)
      SKIP_FRONTEND=true
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

mkdir -p "$LOG_DIR"
touch "$PID_FILE"

load_env() {
  if [[ -f "$ROOT_DIR/.env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source "$ROOT_DIR/.env"
    set +a
  elif [[ -f "$LOCAL_ENV_FILE" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$LOCAL_ENV_FILE"
    set +a
  else
    create_local_env
    set -a
    # shellcheck disable=SC1090
    source "$LOCAL_ENV_FILE"
    set +a
  fi

  export POSTGRES_USER="${POSTGRES_USER:-postgres}"
  export PGADMIN_DEFAULT_EMAIL="${PGADMIN_DEFAULT_EMAIL:-admin@admin.com}"
  export FRONTEND_ALLOWED_ORIGINS="${FRONTEND_ALLOWED_ORIGINS:-http://localhost:3000}"
  export SPRING_KAFKA_BOOTSTRAP_SERVERS="${SPRING_KAFKA_BOOTSTRAP_SERVERS:-localhost:29092}"
  export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-$POSTGRES_USER}"
  export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-$POSTGRES_PASSWORD}"
}

random_secret() {
  od -An -N32 -tx1 /dev/urandom | tr -d ' \n'
}

create_local_env() {
  umask 077
  cat > "$LOCAL_ENV_FILE" <<EOF
POSTGRES_PASSWORD=$(random_secret)
PGADMIN_DEFAULT_PASSWORD=$(random_secret)
JWT_SECRET=$(random_secret)$(random_secret)
INTERNAL_API_KEY=$(random_secret)
GATEWAY_REQUEST_KEY=$(random_secret)
GRAFANA_ADMIN_PASSWORD=$(random_secret)
EOF
  echo "Created local secret file at $LOCAL_ENV_FILE"
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command not found: $1" >&2
    exit 1
  fi
}

port_is_open() {
  local port="$1"
  if command -v nc >/dev/null 2>&1; then
    nc -z 127.0.0.1 "$port" >/dev/null 2>&1
  else
    lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
  fi
}

wait_for_port() {
  local name="$1"
  local port="$2"
  local attempts="${3:-90}"

  printf "Waiting for %-22s on port %s" "$name" "$port"
  for _ in $(seq 1 "$attempts"); do
    if port_is_open "$port"; then
      echo " ready"
      return 0
    fi
    printf "."
    sleep 1
  done
  echo
  echo "$name did not become ready on port $port" >&2
  return 1
}

start_process() {
  local name="$1"
  local port="$2"
  local workdir="$3"
  shift 3
  local log_file="$LOG_DIR/$name.log"
  local pid_tmp="$STATE_DIR/$name.pid"

  if port_is_open "$port"; then
    echo "$name already appears to be running on port $port; not starting a duplicate."
    return 0
  fi

  echo "Starting $name..."
  (
    cd "$workdir"
    nohup "$@" > "$log_file" 2>&1 < /dev/null &
    echo $! > "$pid_tmp"
  )

  local pid
  pid="$(cat "$pid_tmp")"
  rm -f "$pid_tmp"
  grep -v "^$name:" "$PID_FILE" > "$PID_FILE.tmp" || true
  mv "$PID_FILE.tmp" "$PID_FILE"
  echo "$name:$pid" >> "$PID_FILE"
  wait_for_port "$name" "$port" 120
}

validate_local_ports() {
  local required_ports=(8081 8088 8082 8083 8084 8080)

  if [[ "$SKIP_FRONTEND" != "true" ]]; then
    required_ports+=(3000)
  fi

  sleep 2
  for port in "${required_ports[@]}"; do
    if ! port_is_open "$port"; then
      echo "Expected service port $port is not listening after startup." >&2
      return 1
    fi
  done
}

start_infra() {
  local services=(
    postgres-user
    postgres-wallet
    postgres-transaction
    postgres-reward
    postgres-notification
    zookeeper
    kafka
  )

  if port_is_open 6379; then
    echo "Redis already appears to be running on port 6379; not starting a duplicate Docker container."
  else
    services+=(redis)
  fi

  if [[ "$WITH_PGADMIN" == "true" ]]; then
    services+=(pgadmin)
  fi

  echo "Starting Docker infrastructure..."
  docker compose up -d "${services[@]}"

  wait_for_port "postgres-user" 5432 90
  wait_for_port "postgres-wallet" 5433 90
  wait_for_port "postgres-transaction" 5434 90
  wait_for_port "postgres-reward" 5435 90
  wait_for_port "postgres-notification" 5436 90
  wait_for_port "kafka" 29092 120
  wait_for_port "redis" 6379 90
}

start_observability() {
  if [[ "$WITH_OBSERVABILITY" != "true" ]]; then
    return 0
  fi

  echo "Starting observability stack..."
  docker compose -f docker-compose.yml -f docker-compose.observability.yml up -d prometheus grafana
  wait_for_port "prometheus" 9090 90
  wait_for_port "grafana" 3001 90
}

start_local_app() {
  start_infra
  start_observability

  start_process "user-service" 8081 "$ROOT_DIR/user-service" "$ROOT_DIR/mvnw" spring-boot:run -Dspring-boot.run.profiles=local
  start_process "wallet-service" 8088 "$ROOT_DIR/wallet-service" "$ROOT_DIR/mvnw" spring-boot:run -Dspring-boot.run.profiles=local
  start_process "transaction-service" 8082 "$ROOT_DIR/transaction-service" "$ROOT_DIR/mvnw" spring-boot:run -Dspring-boot.run.profiles=local
  start_process "reward-service" 8083 "$ROOT_DIR/reward-service" "$ROOT_DIR/mvnw" spring-boot:run -Dspring-boot.run.profiles=local
  start_process "notification-service" 8084 "$ROOT_DIR/notification-service" "$ROOT_DIR/mvnw" spring-boot:run -Dspring-boot.run.profiles=local
  start_process "api-gateway" 8080 "$ROOT_DIR/api-gateway" "$ROOT_DIR/mvnw" spring-boot:run -Dspring-boot.run.profiles=local

  if [[ "$SKIP_FRONTEND" != "true" ]]; then
    if [[ ! -d "$ROOT_DIR/frontend/node_modules" ]]; then
      echo "Installing frontend dependencies..."
      (cd "$ROOT_DIR/frontend" && npm install)
    fi
    start_process "frontend" 3000 "$ROOT_DIR/frontend" npm run dev
  fi

  validate_local_ports
}

start_docker_app() {
  echo "Starting complete Docker Compose app..."
  local compose_services=(
    postgres-user
    postgres-wallet
    postgres-transaction
    postgres-reward
    postgres-notification
    zookeeper
    kafka
    redis
    user-service
    wallet-service
    transaction-service
    reward-service
    notification-service
    api-gateway
    frontend
  )

  if [[ "$WITH_PGADMIN" == "true" ]]; then
    compose_services+=(pgadmin)
  fi

  echo "Packaging backend services..."
  "$ROOT_DIR/mvnw" -q -DskipTests package

  if [[ "$WITH_OBSERVABILITY" == "true" ]]; then
    compose_services+=(prometheus grafana)
    docker compose -f docker-compose.yml -f docker-compose.observability.yml up -d --build "${compose_services[@]}"
  else
    docker compose up -d --build "${compose_services[@]}"
  fi
  wait_for_port "api-gateway" 8080 180
  wait_for_port "frontend" 3000 180
  if [[ "$WITH_OBSERVABILITY" == "true" ]]; then
    wait_for_port "prometheus" 9090 90
    wait_for_port "grafana" 3001 90
  fi
}

print_summary() {
  cat <<EOF

PayFlow is starting/ready.

Frontend:    http://localhost:3000
API Gateway: http://localhost:8080
Kafka:       localhost:29092
Redis:       localhost:6379
Logs:        $LOG_DIR
PID file:    $PID_FILE

Stop local processes and dependencies with:
  ./stop-payflow.sh
EOF

  if [[ "$WITH_PGADMIN" == "true" ]]; then
    echo "pgAdmin:     http://localhost:5050"
  fi
  if [[ "$WITH_OBSERVABILITY" == "true" ]]; then
    echo "Prometheus:  http://localhost:9090"
    echo "Grafana:     http://localhost:3001"
  fi
}

main() {
  require_command docker
  require_command "$ROOT_DIR/mvnw"
  if [[ "$DOCKER_APP" != "true" && "$SKIP_FRONTEND" != "true" ]]; then
    require_command npm
  fi

  load_env

  if [[ "$DOCKER_APP" == "true" ]]; then
    start_docker_app
  else
    start_local_app
  fi

  print_summary
}

main
