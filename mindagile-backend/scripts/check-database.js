/**
 * Database Check Script
 * 
 * This script checks what tables exist in the database and shows sample data
 * Run: node scripts/check-database.js
 */

const pool = require('../config/database');

async function checkDatabase() {
  try {
    console.log('\n=== DATABASE CHECK ===\n');

    // Check if users table exists and has data
    console.log('1. Checking users table...');
    const usersResult = await pool.query('SELECT COUNT(*) as count FROM users');
    const userCount = usersResult.rows[0].count;
    console.log(`   Total users: ${userCount}`);

    if (userCount > 0) {
      const sampleUsers = await pool.query(
        'SELECT id, email, first_name, last_name, role, created_at FROM users LIMIT 5'
      );
      console.log('   Sample users:');
      sampleUsers.rows.forEach(user => {
        const name = `${user.first_name || ''} ${user.last_name || ''}`.trim() || 'N/A';
        console.log(`   - ID: ${user.id}, Email: ${user.email}, Name: ${name}, Role: ${user.role}`);
      });
    } else {
      console.log('   ⚠️  No users found in database!');
    }

    // Check students table
    console.log('\n2. Checking students table...');
    const studentsResult = await pool.query('SELECT COUNT(*) as count FROM students');
    const studentCount = studentsResult.rows[0].count;
    console.log(`   Total students: ${studentCount}`);

    // Check all tables
    console.log('\n3. Checking all tables...');
    const tablesResult = await pool.query(`
      SELECT table_name 
      FROM information_schema.tables 
      WHERE table_schema = 'public' 
      AND table_type = 'BASE TABLE'
      ORDER BY table_name
    `);

    console.log(`   Found ${tablesResult.rows.length} tables:`);
    for (const row of tablesResult.rows) {
      const tableName = row.table_name;
      const countResult = await pool.query(`SELECT COUNT(*) as count FROM ${tableName}`);
      const count = countResult.rows[0].count;
      console.log(`   - ${tableName}: ${count} rows`);
    }

    // Check if specific tables from schema exist
    console.log('\n4. Checking for required tables...');
    const requiredTables = [
      'users',
      'students',
      'schools',
      'parents',
      'associates',
      'daily_checkins',
      'parent_child_links',
      'assistance_requests',
      'assistance_request_students',
      'fhir_observations',
      'emergency_alerts',
      'audit_logs',
      'appointments',
      'activity_timeline',
      'counselor_notes',
      'interventions'
    ];

    for (const tableName of requiredTables) {
      const existsResult = await pool.query(`
        SELECT EXISTS (
          SELECT FROM information_schema.tables 
          WHERE table_schema = 'public' 
          AND table_name = $1
        )
      `, [tableName]);
      
      const exists = existsResult.rows[0].exists;
      console.log(`   ${exists ? '✅' : '❌'} ${tableName}`);
    }

    console.log('\n=== CHECK COMPLETE ===\n');
    process.exit(0);
  } catch (error) {
    console.error('Error checking database:', error);
    process.exit(1);
  }
}

checkDatabase();

