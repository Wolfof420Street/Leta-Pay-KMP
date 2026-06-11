#!/bin/bash
set -e

# Configuration
HOST="${1:-http://localhost}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
WEB_PORT="${WEB_PORT:-8081}"
SIDECAR_PORT=3100

echo "Starting smoke tests against $HOST..."

# 1. Ktor Backend Health
echo "Checking Ktor Backend..."
HEALTH=$(curl -fsS "${HOST}:${BACKEND_PORT}/health")
if [[ $HEALTH == *"\"misconfigured\":false"* ]]; then
  echo "✅ Ktor Backend is healthy."
else
  echo "❌ Ktor Backend reports misconfiguration: $HEALTH"
  exit 1
fi

# 2. AgentKit Sidecar Health (via internal or external if mapped)
# Note: In prod, sidecar is internal. We skip if not reachable.
echo "Checking Sidecar (if reachable)..."
if curl -fs "${HOST}:${SIDECAR_PORT}/health" > /dev/null; then
  echo "✅ AgentKit Sidecar is healthy."
else
  echo "⚠️ AgentKit Sidecar not reachable on :${SIDECAR_PORT} (expected if internal-only)."
fi

# 3. Web Frontend
echo "Checking Web Frontend..."
if curl -fs "${HOST}:${WEB_PORT}/" > /dev/null; then
  echo "✅ Web Frontend is serving content."
else
  echo "❌ Web Frontend is down on :${WEB_PORT}"
  exit 1
fi

# 4. Prometheus Targets
echo "Checking Prometheus Targets..."
if curl -fs "${HOST}:9090/api/v1/targets" | grep -q "\"health\":\"up\""; then
  echo "✅ Prometheus targets are UP."
else
  echo "⚠️ Prometheus targets not UP yet or not reachable."
fi

echo "All critical smoke tests passed!"
