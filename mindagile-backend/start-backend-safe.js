#!/usr/bin/env node

/**
 * Safe Backend Starter
 * Checks backend status and starts it only if everything is ready
 */

const { spawn } = require('child_process');
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
        resolve(true);
      } else {
        resolve(false);
      }
    });
    
    server.once('listening', () => {
      server.close();
      resolve(false);
    });
    
    server.listen(port);
  });
}

function checkBackendHealth() {
  return new Promise((resolve) => {
    const req = http.get(`${BACKEND_URL}/api/health`, { timeout: 3000 }, (res) => {
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
    
    req.on('error', () => {
      resolve({ success: false, error: 'Connection refused' });
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
    exec(`lsof -ti :${port}`, (error, stdout) => {
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

async function waitForBackend(maxAttempts = 10, delay = 2000) {
  log(`\n⏳ Waiting for backend to be ready...`, 'cyan');
  
  for (let i = 0; i < maxAttempts; i++) {
    await new Promise(resolve => setTimeout(resolve, delay));
    
    const health = await checkBackendHealth();
    if (health.success) {
      log(`✅ Backend is ready!`, 'green');
      return true;
    }
    
    process.stdout.write('.');
  }
  
  log(`\n❌ Backend did not become ready in time`, 'red');
  return false;
}

async function main() {
  log('\n🚀 Safe Backend Starter\n', 'cyan');
  
  // Step 1: Check if port is in use
  log('1️⃣  Checking port availability...', 'blue');
  const portInUse = await checkPortInUse(PORT);
  
  if (portInUse) {
    log(`   ⚠️  Port ${PORT} is in use`, 'yellow');
    
    // Check if it's our backend
    const healthCheck = await checkBackendHealth();
    if (healthCheck.success) {
      log(`   ✅ Backend is already running and healthy!`, 'green');
      log(`   📊 Status: ${healthCheck.health.status}`, 'green');
      
      if (healthCheck.health.databases) {
        log(`   📊 Databases:`, 'cyan');
        Object.entries(healthCheck.health.databases).forEach(([school, status]) => {
          const icon = status.status === 'connected' ? '✅' : '❌';
          log(`      ${icon} ${school}`, status.status === 'connected' ? 'green' : 'red');
        });
      }
      
      log(`\n✅ Backend is already running - no action needed!\n`, 'green');
      process.exit(0);
    } else {
      // Port is in use but not our backend
      const processInfo = await getProcessInfo(PORT);
      log(`   ❌ Port ${PORT} is in use by another process`, 'red');
      if (processInfo) {
        log(`   📋 Process: PID ${processInfo.pid} (${processInfo.user})`, 'yellow');
        log(`   💡 To kill it: sudo kill -9 ${processInfo.pid}\n`, 'yellow');
      }
      process.exit(1);
    }
  } else {
    log(`   ✅ Port ${PORT} is available`, 'green');
  }
  
  // Step 2: Check database connections
  log(`\n2️⃣  Checking database connections...`, 'blue');
  try {
    const dbStatus = await testAllConnections();
    const allConnected = Object.values(dbStatus).every(db => db.status === 'connected');
    
    if (allConnected) {
      log(`   ✅ All databases connected`, 'green');
      Object.entries(dbStatus).forEach(([school, status]) => {
        log(`      ✅ ${school}`, 'green');
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
      log(`   ⚠️  Backend will start but may have limited functionality\n`, 'yellow');
    }
  } catch (error) {
    log(`   ⚠️  Error checking databases: ${error.message}`, 'yellow');
    log(`   ⚠️  Backend will start but may have issues\n`, 'yellow');
  }
  
  // Step 3: Start the backend
  log(`\n3️⃣  Starting backend on port ${PORT}...`, 'blue');
  
  const backendProcess = spawn('node', ['index.js'], {
    stdio: 'inherit',
    env: { ...process.env, PORT: PORT.toString() }
  });
  
  // Handle process exit
  backendProcess.on('exit', (code) => {
    if (code !== 0 && code !== null) {
      log(`\n❌ Backend exited with code ${code}`, 'red');
      process.exit(code);
    }
  });
  
  // Handle errors
  backendProcess.on('error', (error) => {
    log(`\n❌ Failed to start backend: ${error.message}`, 'red');
    process.exit(1);
  });
  
  // Wait for backend to be ready
  const isReady = await waitForBackend();
  
  if (isReady) {
    log(`\n✅ Backend started successfully!`, 'green');
    log(`   🌐 Server: http://0.0.0.0:${PORT}`, 'cyan');
    log(`   📡 WebSocket: ws://0.0.0.0:${PORT}`, 'cyan');
    log(`\n💡 Press Ctrl+C to stop the backend\n`, 'yellow');
    
    // Keep process alive
    process.on('SIGINT', () => {
      log(`\n\n🛑 Stopping backend...`, 'yellow');
      backendProcess.kill('SIGINT');
      process.exit(0);
    });
    
    process.on('SIGTERM', () => {
      log(`\n\n🛑 Stopping backend...`, 'yellow');
      backendProcess.kill('SIGTERM');
      process.exit(0);
    });
  } else {
    log(`\n⚠️  Backend started but health check failed`, 'yellow');
    log(`   Check the logs above for errors\n`, 'yellow');
  }
}

// Run
main().catch((error) => {
  log(`\n❌ Error: ${error.message}`, 'red');
  process.exit(1);
});
