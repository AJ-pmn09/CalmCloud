#!/bin/bash

# Start MindAigle Backend Server
# This script ensures the server listens on 0.0.0.0:3002 for network access

echo "=== Starting MindAigle Backend Server ==="
echo ""

# Set port to 3002
export PORT=3002

# Set environment
export NODE_ENV=production

echo "Configuration:"
echo "  Port: $PORT"
echo "  Host: 0.0.0.0 (all network interfaces)"
echo "  API URL: http://0.0.0.0:$PORT/api"
echo "  Network Access: http://192.168.100.6:$PORT/api"
echo ""

# Check if port is already in use
if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo "⚠️  Port $PORT is already in use!"
    echo "   Stopping existing process..."
    pkill -f "node.*index.js" || true
    sleep 2
fi

# Start the server
echo "Starting server..."
npm start

