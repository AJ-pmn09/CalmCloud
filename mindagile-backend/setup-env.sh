#!/bin/bash

# Setup .env file for multi-database configuration

echo "=========================================="
echo "🔧 Setting up .env file"
echo "=========================================="
echo ""

ENV_FILE=".env"

# Check if .env exists
if [ -f "$ENV_FILE" ]; then
    echo "⚠️  .env file already exists"
    read -p "Do you want to backup and overwrite? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        cp "$ENV_FILE" "${ENV_FILE}.backup.$(date +%Y%m%d_%H%M%S)"
        echo "✅ Backup created"
    else
        echo "❌ Aborted"
        exit 1
    fi
fi

# Generate JWT secret if not set
if [ -z "$JWT_SECRET" ]; then
    echo "🔑 Generating JWT secret..."
    JWT_SECRET=$(openssl rand -base64 32)
    echo "✅ JWT secret generated"
else
    echo "✅ Using existing JWT_SECRET"
fi

# Create .env file
cat > "$ENV_FILE" << EOF
# Master Database (for authentication - if you have one)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=mindaigle
DB_USER=postgres
DB_PASSWORD=postgres

# School Database Configurations
# Using exact names from containers: mindaigle_horizons, mindaigle_houghton, mindaigle_calumet

# Horizons High School (Port 5433)
HORIZONS_DB_HOST=localhost
HORIZONS_DB_PORT=5433
HORIZONS_DB_NAME=mindaigle_horizons
HORIZONS_DB_USER=postgres
HORIZONS_DB_PASSWORD=postgres

# Houghton High School (Port 5436)
HOUGHTON_DB_HOST=localhost
HOUGHTON_DB_PORT=5436
HOUGHTON_DB_NAME=mindaigle_houghton
HOUGHTON_DB_USER=postgres
HOUGHTON_DB_PASSWORD=postgres

# Calumet Middle School (Port 5434)
CALUMET_DB_HOST=localhost
CALUMET_DB_PORT=5434
CALUMET_DB_NAME=mindaigle_calumet
CALUMET_DB_USER=postgres
CALUMET_DB_PASSWORD=postgres

# Server Configuration
PORT=3002
NODE_ENV=production

# JWT Secret
JWT_SECRET=$JWT_SECRET
EOF

echo ""
echo "✅ .env file created successfully!"
echo ""
echo "📋 Configuration:"
echo "   - Horizons: mindaigle_horizons (port 5433)"
echo "   - Houghton: mindaigle_houghton (port 5436)"
echo "   - Calumet: mindaigle_calumet (port 5434)"
echo "   - JWT Secret: Generated"
echo ""
echo "🚀 Next steps:"
echo "   1. Review .env file: cat .env"
echo "   2. Test connections: npm run test-db"
echo "   3. Start backend: PORT=3002 npm start"
echo ""

