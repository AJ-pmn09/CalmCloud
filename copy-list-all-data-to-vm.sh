#!/usr/bin/env bash
# Copy list-all-data.js to the K-12 VM so you can run it there.
# Run this from your Mac (Mindagile repo root).
#
# You must set the VM's IP or hostname (MH is the VM's local name and won't resolve from your Mac):
#   VM_HOST=192.168.1.100 ./copy-list-all-data-to-vm.sh
# Or export it first:
#   export VM_HOST=192.168.1.100
#   ./copy-list-all-data-to-vm.sh
set -e
VM_USER="${VM_USER:-vkasarla}"
VM_HOST="${VM_HOST:?Set VM_HOST to the VM IP or hostname, e.g. VM_HOST=192.168.1.100 ./copy-list-all-data-to-vm.sh}"
VM_PATH="/home/vkasarla/Desktop/K-12/backend/scripts"
REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
SCRIPTS=(
  "list-all-data.js"
  "ensure-tables-all-schools.js"
  "seed-messages-standalone.js"
)
for name in "${SCRIPTS[@]}"; do
  SCRIPT="$REPO_ROOT/mindagile-backend/scripts/$name"
  if [[ ! -f "$SCRIPT" ]]; then
    echo "Not found: $SCRIPT"
    exit 1
  fi
done
echo "Copying scripts to ${VM_USER}@${VM_HOST}:${VM_PATH}/"
for name in "${SCRIPTS[@]}"; do
  scp "$REPO_ROOT/mindagile-backend/scripts/$name" "${VM_USER}@${VM_HOST}:${VM_PATH}/"
done
echo "Done. On the VM run:"
echo "  cd /home/vkasarla/Desktop/K-12/backend"
echo "  node scripts/list-all-data.js          # list all DB data"
echo "  node scripts/ensure-tables-all-schools.js   # create missing tables (Houghton/Calumet)"
echo "  node scripts/seed-messages-standalone.js   # seed test messages for Messages screen"
