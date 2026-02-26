#!/bin/bash
# Copy login-token middleware to VM backend, then add two lines to the main server file.
# Run from repo root or Mindagile. Uses VM host 192.168.100.6 and user vkasarla.

set -e
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VM_USER="${VM_USER:-vkasarla}"
VM_HOST="${VM_HOST:-192.168.100.6}"
VM_BACKEND="${VM_BACKEND:-/home/vkasarla/Desktop/K-12/backend}"

echo "Copying add-login-token-middleware.js and add-middleware-to-server.js to VM backend..."
scp "$REPO_ROOT/vm-files/add-login-token-middleware.js" "$REPO_ROOT/vm-files/add-middleware-to-server.js" "$VM_USER@$VM_HOST:$VM_BACKEND/"

echo ""
echo "Done. On the VM, run (auto-adds middleware to the correct server file):"
echo ""
echo "  ssh $VM_USER@$VM_HOST"
echo "  cd $VM_BACKEND"
echo "  npm install jsonwebtoken"
echo "  node add-middleware-to-server.js"
echo "  cd /home/vkasarla/Desktop/K-12 && ./STOP_ALL.sh && ./START_ALL.sh"
echo ""
echo "Then test login (use the port START_ALL shows, e.g. 3009):"
echo "  curl -s -X POST http://localhost:3009/api/auth/login -H 'Content-Type: application/json' -d '{\"email\":\"emma.thompson@horizons.edu\",\"password\":\"Student123!\"}'"
echo ""
echo "You should see \"token\":\"eyJ...\" in the response."
echo ""
