#!/usr/bin/env bash
#
# Start Mindagile/Clamcloud backend for K-12 Docker DB setup (vkasrla).
# When vkasrla starts DBs (START_ALL.sh), we connect to the same DBs via .env (ports 5433, 5436, 5434).
# Backend listens on port 3002.
#

set -e
cd "$(dirname "$0")"

echo "=== Mindagile Backend (K-12 Docker DB) ==="

# Load .env first (DB credentials, USE_SINGLE_DB, PORT, etc.)
if [ -f .env ]; then
  set -a
  source .env
  set +a
  echo "Using .env"
else
  echo "⚠️  No .env found. Copy .env.k12-docker.example to .env and edit if needed."
  if [ -f .env.k12-docker.example ]; then
    cp .env.k12-docker.example .env
    source .env
    echo "   Created .env from .env.k12-docker.example"
  fi
fi

# Optional: source vkasrla's ports file for DB ports (if readable)
PORTS_FILE=""
for f in /tmp/mindaigle_db_ports.*.env; do
  [ -f "$f" ] && [ -r "$f" ] && PORTS_FILE="$f" && break
done
if [ -n "$PORTS_FILE" ]; then
  echo "Sourcing DB ports from: $PORTS_FILE"
  set -a
  source "$PORTS_FILE"
  set +a
else
  if ls /tmp/mindaigle_db_ports.*.env 1>/dev/null 2>&1; then
    echo "Ports file exists but not readable; using .env (DB ports 5433, 5436, 5434)."
  else
    echo "Using .env for DB ports (5433, 5436, 5434)."
  fi
fi

# Backend HTTP port: always 3002 (override .env so we don't pick up PORT=3003 from an old config)
export PORT=3002
export NODE_ENV="${NODE_ENV:-production}"

echo "Backend port: $PORT | NODE_ENV: $NODE_ENV"
echo ""

# Start backend (connects to DBs on 5433, 5436, 5434 from .env)
exec node index.js
