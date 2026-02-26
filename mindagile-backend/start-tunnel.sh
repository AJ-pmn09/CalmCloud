#!/bin/bash

# Start Tunnel Script for MindAigle Backend
# This script starts a localtunnel to expose the backend API to the internet

PORT=${1:-3002}
TUNNEL_NAME=${2:-""}

echo "=========================================="
echo "🚇 Starting Tunnel for MindAigle Backend"
echo "=========================================="
echo ""
echo "Backend Port: $PORT"
echo ""

# Check if backend is running
if ! curl -s http://localhost:$PORT/api/health > /dev/null 2>&1; then
    echo "⚠️  WARNING: Backend doesn't seem to be running on port $PORT"
    echo "   Make sure backend is started first!"
    echo ""
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Check if localtunnel is installed
if ! command -v lt &> /dev/null; then
    echo "📦 Installing localtunnel..."
    npm install -g localtunnel
fi

# Start tunnel
echo "🌐 Starting tunnel..."
echo ""

if [ -z "$TUNNEL_NAME" ]; then
    # Random tunnel name
    lt --port $PORT
else
    # Custom tunnel name (if available)
    lt --port $PORT --subdomain $TUNNEL_NAME
fi

echo ""
echo "=========================================="
echo "✅ Tunnel is running!"
echo ""
echo "⚠️  IMPORTANT:"
echo "   1. Keep this terminal open!"
echo "   2. Copy the tunnel URL above"
echo "   3. Update your app with the tunnel URL"
echo "   4. First-time: Visit URL in browser to activate"
echo "=========================================="

