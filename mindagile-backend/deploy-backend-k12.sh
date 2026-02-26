#!/usr/bin/env bash
#
# Deploy Mindagile backend files for K-12 Docker DB into a target directory.
# Usage: ./deploy-backend-k12.sh [TARGET_DIR]
#   TARGET_DIR defaults to ../clamcloud-backend (relative to mindagile-backend) or . if run from target.
# Run from mindagile-backend/ or pass full path to target.
#

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

TARGET="${1:-}"
if [ -z "$TARGET" ]; then
  if [ -d "$SCRIPT_DIR/../clamcloud-backend" ]; then
    TARGET="$SCRIPT_DIR/../clamcloud-backend"
  else
    TARGET="$SCRIPT_DIR"
    echo "No TARGET_DIR given; using script directory"
  fi
fi
if [ ! -d "$TARGET" ]; then
  if [ -d "$SCRIPT_DIR/$TARGET" ]; then
    TARGET="$(cd "$SCRIPT_DIR/$TARGET" && pwd)"
  else
    echo "Target directory does not exist: $TARGET"
    exit 1
  fi
else
  TARGET="$(cd "$TARGET" && pwd)"
fi
echo "Deploying K-12 backend files into: $TARGET"

# Required files/dirs
mkdir -p "$TARGET/config" "$TARGET/routes" "$TARGET/middleware" "$TARGET/jobs" "$TARGET/websocket" "$TARGET/scripts" "$TARGET/services"

cp -f config/databaseManager.js    "$TARGET/config/"
cp -f config/database.js         "$TARGET/config/"
cp -f routes/auth.js              "$TARGET/routes/"
cp -f routes/studentData.js      "$TARGET/routes/"
cp -f routes/analytics.js        "$TARGET/routes/"
cp -f routes/assistance.js       "$TARGET/routes/"
cp -f routes/alerts.js           "$TARGET/routes/"
cp -f routes/reminders.js        "$TARGET/routes/"
cp -f routes/communications.js   "$TARGET/routes/"
cp -f routes/appointments.js     "$TARGET/routes/"
cp -f routes/achievements.js     "$TARGET/routes/"
cp -f routes/counselorNotes.js    "$TARGET/routes/"
cp -f routes/activityLogs.js      "$TARGET/routes/"
cp -f routes/staffReplies.js     "$TARGET/routes/"
cp -f routes/fhirExport.js       "$TARGET/routes/"
# Clinical screeners (PHQ-9 / GAD-7)
[ -f routes/screeners.js ] && cp -f routes/screeners.js "$TARGET/routes/" || true
[ -f services/screenerScoring.js ] && cp -f services/screenerScoring.js "$TARGET/services/" || true
cp -f middleware/auth.js          "$TARGET/middleware/"
cp -f middleware/databaseRouter.js "$TARGET/middleware/"
cp -f index.js                   "$TARGET/"
cp -f package.json               "$TARGET/"
cp -f start-backend-k12.sh       "$TARGET/"
cp -f .env.k12-docker.example    "$TARGET/"

# Optional but useful
[ -f jobs/reminderScheduler.js ] && cp -f jobs/reminderScheduler.js "$TARGET/jobs/" || true
[ -d websocket ] && cp -f websocket/socketHandler.js "$TARGET/websocket/" 2>/dev/null || true

# Create .env from example if missing
if [ ! -f "$TARGET/.env" ]; then
  cp "$TARGET/.env.k12-docker.example" "$TARGET/.env"
  echo "Created $TARGET/.env from .env.k12-docker.example"
else
  echo "Keeping existing $TARGET/.env"
fi

chmod +x "$TARGET/start-backend-k12.sh" 2>/dev/null || true
echo "Done. Next: cd $TARGET && npm install && ./start-backend-k12.sh"
