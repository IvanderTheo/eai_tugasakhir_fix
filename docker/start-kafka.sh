#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Starting Kafka (Docker)..."
docker compose up -d kafka

echo "Waiting for Kafka to be healthy..."
for i in $(seq 1 60); do
  if docker compose ps kafka 2>/dev/null | grep -qi healthy; then
    echo "Kafka is ready on localhost:9092"
    docker compose ps kafka
    exit 0
  fi
  sleep 2
done

echo "Kafka did not become healthy in time. Check: docker compose logs kafka"
exit 1
