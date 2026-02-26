#!/usr/bin/env node

/**
 * Test script to verify all database connections
 */

require('dotenv').config();
const { testAllConnections } = require('./config/databaseManager');

async function main() {
  console.log('==========================================');
  console.log('🧪 Testing Database Connections');
  console.log('==========================================');
  console.log('');

  try {
    const results = await testAllConnections();

    console.log('📊 Connection Results:');
    console.log('');

    let allConnected = true;
    for (const [school, result] of Object.entries(results)) {
      const status = result.status === 'connected' ? '✅' : '❌';
      console.log(`${status} ${school.toUpperCase()}: ${result.status}`);
      
      if (result.status === 'connected') {
        console.log(`   Timestamp: ${result.timestamp}`);
      } else {
        console.log(`   Error: ${result.error}`);
        allConnected = false;
      }
      console.log('');
    }

    console.log('==========================================');
    if (allConnected) {
      console.log('✅ All databases connected successfully!');
      process.exit(0);
    } else {
      console.log('❌ Some databases failed to connect');
      console.log('   Please check your .env file and container status');
      process.exit(1);
    }
  } catch (error) {
    console.error('❌ Error testing connections:', error);
    process.exit(1);
  }
}

main();

