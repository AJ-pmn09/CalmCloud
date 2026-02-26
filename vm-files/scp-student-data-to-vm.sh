#!/bin/bash
# Copy studentData route and add-student-data-route.js to VM so GET /api/student-data/me and /trends work.
# Run from repo root (Mindagile). Uses VM host 192.168.100.6 and user vkasarla.

set -e
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VM_USER="${VM_USER:-vkasarla}"
VM_HOST="${VM_HOST:-192.168.100.6}"
VM_BACKEND="${VM_BACKEND:-/home/vkasarla/Desktop/K-12/backend}"

echo "Ensuring backend/routes, middleware, config exist on VM..."
ssh "$VM_USER@$VM_HOST" "mkdir -p $VM_BACKEND/routes $VM_BACKEND/src/routes $VM_BACKEND/middleware $VM_BACKEND/config"

echo "Copying routes/studentData.js to VM..."
scp "$REPO_ROOT/mindagile-backend/routes/studentData.js" "$VM_USER@$VM_HOST:$VM_BACKEND/routes/"
scp "$REPO_ROOT/mindagile-backend/routes/studentData.js" "$VM_USER@$VM_HOST:$VM_BACKEND/src/routes/"

echo "Copying middleware (auth, databaseRouter) so studentData can load..."
scp "$REPO_ROOT/mindagile-backend/middleware/auth.js" "$REPO_ROOT/mindagile-backend/middleware/databaseRouter.js" "$VM_USER@$VM_HOST:$VM_BACKEND/middleware/"

echo "Copying config/databaseManager.js (do not overwrite config/database.js - VM may use different DB ports)..."
scp "$REPO_ROOT/mindagile-backend/config/databaseManager.js" "$VM_USER@$VM_HOST:$VM_BACKEND/config/"

echo "Copying add-student-data-route.js to VM..."
scp "$REPO_ROOT/vm-files/add-student-data-route.js" "$VM_USER@$VM_HOST:$VM_BACKEND/"

echo ""
echo "On the VM, run (fixes require path to backend/routes so middleware loads, then restart):"
echo "  ssh $VM_USER@$VM_HOST"
echo "  cd $VM_BACKEND"
echo "  node add-student-data-route.js"
echo "  cd /home/vkasarla/Desktop/K-12 && ./STOP_ALL.sh && ./START_ALL.sh"
echo ""
echo "Backend may listen on a different port (e.g. 3003 or 3004). Check START_ALL output."
echo "Then test (replace PORT with backend port):"
echo "  curl -s http://localhost:PORT/api/health"
echo "  curl -s -X POST http://localhost:PORT/api/auth/login -H 'Content-Type: application/json' -d '{\"email\":\"emma.thompson@horizons.edu\",\"password\":\"Student123!\"}'"
echo "  TOKEN=\"<paste token from response>\""
echo "  curl -s -H \"Authorization: Bearer \$TOKEN\" \"http://localhost:PORT/api/student-data/me\""
echo ""
