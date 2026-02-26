#!/bin/bash
# Run screener migration on each school DB used by the backend (horizons, houghton, calumet).
# Use the same ports your clamcloud-backend .env uses for each school.
# If your DB names or ports differ, edit below.

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MIGRATE="$SCRIPT_DIR/migrate-screeners.sql"

run_migrate() {
  local db_name=$1
  local port=${2:-5432}
  echo "Running migrate-screeners.sql on database: $db_name (port $port)"
  psql -h localhost -p "$port" -U postgres -d "$db_name" -f "$MIGRATE" || true
}

# Adjust DB names and ports to match your VM config (see backend .env or databaseManager)
run_migrate "horizons" "5433"
run_migrate "houghton" "5436"
run_migrate "calumet" "5434"

echo "Done. If submit still fails, drop screener tables in the affected DB and re-run this script."
