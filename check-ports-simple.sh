#!/bin/bash

# Simple Port Checker - Quick Version
# Run on VM: bash check-ports-simple.sh

echo "🔍 Quick Port Check"
echo "=================="
echo ""

echo "Backend (3003):"
sudo lsof -i :3003 && echo "✅ Running" || echo "❌ Not running"
echo ""

echo "Databases:"
echo "  Horizons (5433):" && sudo lsof -i :5433 > /dev/null 2>&1 && echo "    ✅ Running" || echo "    ❌ Not running"
echo "  Calumet (5434):" && sudo lsof -i :5434 > /dev/null 2>&1 && echo "    ✅ Running" || echo "    ❌ Not running"
echo "  Houghton (5436):" && sudo lsof -i :5436 > /dev/null 2>&1 && echo "    ✅ Running" || echo "    ❌ Not running"
echo ""

echo "All Ports Summary:"
netstat -tuln | grep -E ':(3003|5433|5434|5436)' | awk '{print "  " $4 " - " $1}'
