#!/bin/bash
# Run screener migration (PHQ-9/GAD-7 tables) on all three school databases
# so all three schools get the same schema updates.

set -e

# Where the SQL file lives (same directory as this script)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="${SCRIPT_DIR}/migrate-screeners.sql"

if [ ! -f "$SQL_FILE" ]; then
  echo "Error: $SQL_FILE not found."
  exit 1
fi

# Connection defaults (match mindagile-backend/config/databaseManager.js)
# Override with env vars if your setup is different (e.g. on VM):
#   export DB_HOST=localhost
#   export DB_PORT=5432
#   export DB_USER=postgres
#   export DB_PASSWORD=postgres
DB_HOST="${DB_HOST:-localhost}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

# Three school databases
# If DB_PORT is set, use it for all three (one Postgres, three DBs on same port).
# Otherwise use separate ports per school (three Postgres instances).
if [ -n "${DB_PORT}" ]; then
  HORIZONS_PORT="$DB_PORT"
  HOUGHTON_PORT="$DB_PORT"
  CALUMET_PORT="$DB_PORT"
else
  HORIZONS_PORT="${HORIZONS_DB_PORT:-5433}"
  HOUGHTON_PORT="${HOUGHTON_DB_PORT:-5436}"
  CALUMET_PORT="${CALUMET_DB_PORT:-5434}"
fi

run_migrate() {
  local name=$1
  local port=$2
  local db=$3
  echo "----------------------------------------"
  echo "Running migration: $name (port $port, db $db)"
  echo "----------------------------------------"
  if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$port" -U "$DB_USER" -d "$db" -f "$SQL_FILE"; then
    echo "OK: $name"
  else
    echo "FAILED: $name"
    return 1
  fi
}

echo "Applying migrate-screeners.sql to all three schools..."
run_migrate "Horizons"  "$HORIZONS_PORT" "mindaigle_horizons"
run_migrate "Houghton"  "$HOUGHTON_PORT" "mindaigle_houghton"
run_migrate "Calumet"   "$CALUMET_PORT"  "mindaigle_calumet"

echo ""
echo "Done. All three schools have the same screener schema updates."
