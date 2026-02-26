#!/bin/bash

# Mindagile Server Configuration Script
# Configures the Android app to connect to your backend server

echo "=== Mindagile Server Configuration ==="
echo ""

# Get server IP
echo "Step 1: Finding server IP address..."
SERVER_IP=$(hostname -I | awk '{print $1}')

if [ -z "$SERVER_IP" ]; then
    echo "Could not automatically detect IP. Please enter your server IP:"
    read -p "Server IP: " SERVER_IP
else
    echo "Detected server IP: $SERVER_IP"
    read -p "Use this IP? (y/n): " confirm
    if [ "$confirm" != "y" ]; then
        read -p "Enter server IP: " SERVER_IP
    fi
fi

echo ""
echo "Step 2: Updating Android app configuration..."

# Update build.gradle.kts
GRADLE_FILE="app/build.gradle.kts"
if [ -f "$GRADLE_FILE" ]; then
    # Backup original
    cp "$GRADLE_FILE" "${GRADLE_FILE}.backup"
    
    # Update SERVER_URL
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        sed -i '' "s|buildConfigField(\"String\", \"SERVER_URL\", \".*\"|buildConfigField(\"String\", \"SERVER_URL\", \"\\\"http://${SERVER_IP}\\\"\")|g" "$GRADLE_FILE"
    else
        # Linux
        sed -i "s|buildConfigField(\"String\", \"SERVER_URL\", \".*\"|buildConfigField(\"String\", \"SERVER_URL\", \"\\\"http://${SERVER_IP}\\\"\")|g" "$GRADLE_FILE"
    fi
    
    echo "✓ Updated $GRADLE_FILE"
else
    echo "✗ Could not find $GRADLE_FILE"
fi

# Update network_security_config.xml
NETWORK_CONFIG="app/src/main/res/xml/network_security_config.xml"
if [ -f "$NETWORK_CONFIG" ]; then
    # Backup original
    cp "$NETWORK_CONFIG" "${NETWORK_CONFIG}.backup"
    
    # Update domain
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        sed -i '' "s|<domain includeSubdomains=\"true\">.*</domain>|<domain includeSubdomains=\"true\">${SERVER_IP}</domain>|g" "$NETWORK_CONFIG"
    else
        # Linux
        sed -i "s|<domain includeSubdomains=\"true\">.*</domain>|<domain includeSubdomains=\"true\">${SERVER_IP}</domain>|g" "$NETWORK_CONFIG"
    fi
    
    echo "✓ Updated $NETWORK_CONFIG"
else
    echo "✗ Could not find $NETWORK_CONFIG"
fi

echo ""
echo "Step 3: Verifying server connectivity..."

# Test if backend is accessible
if command -v curl &> /dev/null; then
    echo "Testing backend connection..."
    if curl -s -o /dev/null -w "%{http_code}" "http://${SERVER_IP}:3002/api/hello" | grep -q "200"; then
        echo "✓ Backend is accessible at http://${SERVER_IP}:3002"
    else
        echo "⚠ Backend might not be running or accessible"
        echo "  Make sure backend is running: cd mindagile-backend && ./start-server.sh"
    fi
else
    echo "⚠ curl not found, skipping connectivity test"
fi

echo ""
echo "=== Configuration Complete ==="
echo ""
echo "Summary:"
echo "  Server IP: $SERVER_IP"
echo "  Server Port: 3002"
echo "  API URL: http://${SERVER_IP}:3002/api"
echo ""
echo "Next steps:"
echo "  1. Rebuild the app: ./gradlew clean build"
echo "  2. Install on device/emulator"
echo "  3. Test login and data sync"
echo ""
echo "Backup files created:"
echo "  - ${GRADLE_FILE}.backup"
echo "  - ${NETWORK_CONFIG}.backup"

