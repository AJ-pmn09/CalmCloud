/**
 * List All Users Script
 * 
 * This script lists all users in the database
 * Run: node scripts/list-users.js
 */

const pool = require('../config/database');

async function listUsers() {
  try {
    console.log('\n=== ALL USERS IN DATABASE ===\n');

    const result = await pool.query(
      'SELECT id, email, name, role, created_at FROM users ORDER BY id'
    );

    if (result.rows.length === 0) {
      console.log('No users found in database.\n');
      process.exit(0);
    }

    console.log(`Total users: ${result.rows.length}\n`);
    
    result.rows.forEach((user, index) => {
      console.log(`${index + 1}. User ID: ${user.id}`);
      console.log(`   Email: ${user.email}`);
      console.log(`   Name: ${user.name}`);
      console.log(`   Role: ${user.role}`);
      console.log(`   Created: ${user.created_at}`);
      console.log('');
    });

    process.exit(0);
  } catch (error) {
    console.error('Error listing users:', error);
    process.exit(1);
  }
}

listUsers();

