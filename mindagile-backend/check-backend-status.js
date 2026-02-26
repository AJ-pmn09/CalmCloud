#!/usr/bin/env node

/**
 * Backend Status Checker
 * Checks if port 3002 is available and verifies backend health
 */

const http = require('http');
const { testAllConnections } = require('./config/databaseManager');

// Force port 3002 (backend was moved from 3003 to 3002)
const PORT = 3002;
const BACKEND_URL = `http://localhost:${PORT}`;

// Colors for terminal output
const colors = {
  reset: '\x1b[0m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m',
};

function log(message, color = 'reset') {
  console.log(`${colors[color]}${message}${colors.reset}`);
}

function checkPortInUse(port) {
  return new Promise((resolve) => {
    const server = require('net').createServer();
    
    server.once('error', (err) => {
      if (err.code === 'EADDRINUSE') {
        resolve(true); // Port is in use
      } else {
        resolve(false); // Different error, port might be available
      }
    });
    
    server.once('listening', () => {
      server.close();
      resolve(false); // Port is available
    });
    
    server.listen(port);
  });
}

function checkBackendHealth() {
  return new Promise((resolve) => {
    const req = http.get(`${BACKEND_URL}/api/health`, { timeout: 5000 }, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        try {
          const health = JSON.parse(data);
          resolve({ success: true, health });
        } catch (e) {
          resolve({ success: false, error: 'Invalid JSON response' });
        }
      });
    });
    
    req.on('error', (err) => {
      resolve({ success: false, error: err.message });
    });
    
    req.on('timeout', () => {
      req.destroy();
      resolve({ success: false, error: 'Request timeout' });
    });
  });
}

function getProcessInfo(port) {
  return new Promise((resolve) => {
    const { exec } = require('child_process');
    exec(`lsof -ti :${port}`, (error, stdout, stderr) => {
      if (error || !stdout.trim()) {
        resolve(null);
        return;
      }
      
      const pid = stdout.trim();
      exec(`ps -p ${pid} -o pid,user,command --no-headers`, (err, output) => {
        if (err) {
          resolve({ pid });
        } else {
          const parts = output.trim().split(/\s+/);
          resolve({
            pid,
            user: parts[1] || 'unknown',
            command: parts.slice(2).join(' ') || 'unknown'
          });
        }
      });
    });
  });
}

async function main() {
  log('\n🔍 Checking Backend Status...\n', 'cyan');
  
  // Check 1: Port availability
  log('1️⃣  Checking if port 3002 is available...', 'blue');
  const portInUse = await checkPortInUse(PORT);
  
  if (portInUse) {
    log(`   ❌ Port ${PORT} is already in use`, 'red');
    
    // Get process info
    const processInfo = await getProcessInfo(PORT);
    if (processInfo) {
      log(`   📋 Process Details:`, 'yellow');
      log(`      PID: ${processInfo.pid}`, 'yellow');
      log(`      User: ${processInfo.user}`, 'yellow');
      log(`      Command: ${processInfo.command.substring(0, 80)}...`, 'yellow');
    }
    
    // Check if it's actually the backend
    log(`\n2️⃣  Checking if backend is responding...`, 'blue');
    const healthCheck = await checkBackendHealth();
    
    if (healthCheck.success) {
      log(`   ✅ Backend is running and responding!`, 'green');
      log(`   📊 Health Status: ${healthCheck.health.status}`, 'green');
      
      if (healthCheck.health.databases) {
        log(`   📊 Database Status:`, 'cyan');
        Object.entries(healthCheck.health.databases).forEach(([school, status]) => {
          const icon = status.status === 'connected' ? '✅' : '❌';
          log(`      ${icon} ${school}: ${status.status}`, 
              status.status === 'connected' ? 'green' : 'red');
        });
      }
      
      log(`\n✅ Backend is healthy and running on port ${PORT}`, 'green');
      log(`   No action needed!\n`, 'green');
      process.exit(0);
    } else {
      log(`   ❌ Port ${PORT} is in use but backend is not responding`, 'red');
      log(`   Error: ${healthCheck.error}`, 'red');
      log(`\n⚠️  Recommendation:`, 'yellow');
      log(`   Kill the process using port ${PORT}:`, 'yellow');
      log(`   sudo kill -9 ${processInfo?.pid || '<PID>'}\n`, 'yellow');
      process.exit(1);
    }
  } else {
    log(`   ✅ Port ${PORT} is available`, 'green');
  }
  
  // Check 2: Database connections
  log(`\n3️⃣  Checking database connections...`, 'blue');
  try {
    const dbStatus = await testAllConnections();
    const allConnected = Object.values(dbStatus).every(db => db.status === 'connected');
    
    if (allConnected) {
      log(`   ✅ All databases connected`, 'green');
      Object.entries(dbStatus).forEach(([school, status]) => {
        log(`      ✅ ${school}: ${status.status}`, 'green');
      });
    } else {
      log(`   ⚠️  Some databases are not connected:`, 'yellow');
      Object.entries(dbStatus).forEach(([school, status]) => {
        const icon = status.status === 'connected' ? '✅' : '❌';
        const color = status.status === 'connected' ? 'green' : 'red';
        log(`      ${icon} ${school}: ${status.status}`, color);
        if (status.error) {
          log(`         Error: ${status.error}`, 'red');
        }
      });
    }
  } catch (error) {
    log(`   ❌ Error checking databases: ${error.message}`, 'red');
  }
  
  // Summary
  log(`\n📋 Summary:`, 'cyan');
  if (!portInUse) {
    log(`   ✅ Port ${PORT} is available - Ready to start backend`, 'green');
    log(`\n🚀 To start the backend:`, 'cyan');
    log(`   cd ~/mindaigle-backend`, 'yellow');
    log(`   PORT=${PORT} npm start\n`, 'yellow');
  } else {
    log(`   ⚠️  Port ${PORT} is in use`, 'yellow');
    log(`   Check the process details above\n`, 'yellow');
  }
}

// Run the check
main().catch((error) => {
  log(`\n❌ Error: ${error.message}`, 'red');
  process.exit(1);
});
