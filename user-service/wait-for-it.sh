#!/bin/sh
# wait-for-it.sh

set -e

host="$1"
port="$2"
shift 2
cmd="$@"

echo "Waiting for $host:$port to be ready..."
while ! nc -z "$host" "$port"; do
  echo "Still waiting for $host:$port..."
  sleep 2
done

echo "$host:$port is available!"
exec $cmd