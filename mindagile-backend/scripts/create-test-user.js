/**
 * Create Test User Script
 * 
 * This script creates a test user for login testing
 * Run: node scripts/create-test-user.js
 * 
 * Creates a user with:
 * Email: test@example.com
 * Password: test123
 * Role: student
 */

const pool = require('../config/database');
const bcrypt = require('bcrypt');

async function createTestUser() {
  try {
    const email = 'test@example.com';
    const password = 'test123';
    const name = 'Test User';
    const role = 'student';

    console.log('\n=== Creating Test User ===\n');
    console.log(`Email: ${email}`);
    console.log(`Password: ${password}`);
    console.log(`Name: ${name}`);
    console.log(`Role: ${role}\n`);

    // Check if user exists
    const existingUser = await pool.query(
      'SELECT id, email FROM users WHERE email = $1',
      [email]
    );

    if (existingUser.rows.length > 0) {
      console.log('⚠️  User already exists!');
      console.log(`   User ID: ${existingUser.rows[0].id}`);
      console.log('\nTo reset password, delete the user first or use a different email.\n');
      process.exit(0);
    }

    // Hash password
    const saltRounds = 10;
    const passwordHash = await bcrypt.hash(password, saltRounds);

    // Create user
    const result = await pool.query(
      `INSERT INTO users (email, password_hash, name, role) 
       VALUES ($1, $2, $3, $4) 
       RETURNING id, email, name, role, created_at`,
      [email, passwordHash, name, role]
    );

    const user = result.rows[0];

    console.log('✅ User created successfully!');
    console.log(`   User ID: ${user.id}`);
    console.log(`   Email: ${user.email}`);
    console.log(`   Name: ${user.name}`);
    console.log(`   Role: ${user.role}`);
    console.log(`   Created: ${user.created_at}\n`);

    // If student, create student profile
    if (role === 'student') {
      const studentResult = await pool.query(
        `INSERT INTO students (user_id, first_name, last_name, grade) 
         VALUES ($1, $2, $3, $4) 
         RETURNING id`,
        [user.id, 'Test', 'Student', 10]
      );
      console.log(`✅ Student profile created!`);
      console.log(`   Student ID: ${studentResult.rows[0].id}\n`);
    }

    console.log('You can now login with:');
    console.log(`   Email: ${email}`);
    console.log(`   Password: ${password}\n`);

    process.exit(0);
  } catch (error) {
    console.error('Error creating test user:', error);
    process.exit(1);
  }
}

createTestUser();

