#!/bin/bash
# Start the backend on port 3006 so the app (configured for 3006) works.
# Copy to VM, chmod +x, then run from K-12 folder (or run with full path).

set -e
K12_DIR="${K12_DIR:-/home/vkasarla/Desktop/K-12}"
BACKEND_DIR="$K12_DIR/backend"
export PORT=3006

# If this script lives inside K-12, use that as K12_DIR
if [ -n "$BASH_SOURCE" ]; then
  SCRIPT_DIR="$(cd "$(dirname "$BASH_SOURCE")" && pwd)"
  if [ -f "$SCRIPT_DIR/start-backend-host.sh" ] || [ -f "$SCRIPT_DIR/START_ALL.sh" ]; then
    K12_DIR="$SCRIPT_DIR"
  elif [ -f "$SCRIPT_DIR/../START_ALL.sh" ]; then
    K12_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
  fi
  BACKEND_DIR="$K12_DIR/backend"
fi

cd "$K12_DIR" || { echo "Error: could not cd to $K12_DIR"; exit 1; }

# Load env and DB ports (skip port file if not readable, e.g. permission denied)
[ -f .env ] && set -a && source .env && set +a
for f in /tmp/mindaigle_db_ports.*.env; do
  if [ -f "$f" ] && [ -r "$f" ]; then
    set -a; source "$f" 2>/dev/null || true; set +a; break
  fi
done

echo "Starting backend on PORT=3006..."

# Option 1: Use start script if it respects PORT
if [ -f ./start-backend-host.sh ]; then
  ./start-backend-host.sh
  exit 0
fi

# Option 2: Start Node directly so PORT=3006 is guaranteed (runs in background)
cd "$BACKEND_DIR" || { echo "Error: no $BACKEND_DIR"; exit 1; }
LOG="${LOG:-$K12_DIR/backend.log}"
ENTRY=""
[ -f index.js ] && ENTRY="index.js"
[ -z "$ENTRY" ] && [ -f src/server.js ] && ENTRY="src/server.js"
[ -z "$ENTRY" ] && [ -f src/index.js ] && ENTRY="src/index.js"
if [ -z "$ENTRY" ]; then
  echo "Error: no index.js or src/server.js in $BACKEND_DIR"
  exit 1
fi
nohup node "$ENTRY" >> "$LOG" 2>&1 &
echo "Backend started on port 3006 (PID $!). Log: $LOG"
echo "Check: curl -s http://localhost:3006/api/health"
