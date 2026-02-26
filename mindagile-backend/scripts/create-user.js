/**
 * Create User Script
 * 
 * Usage: node scripts/create-user.js <email> <password> <name> <role>
 * Example: node scripts/create-user.js emma.thompson@horizon.edu password123 "Emma Thompson" student
 */

const pool = require('../config/database');
const bcrypt = require('bcrypt');

async function createUser() {
  try {
    const args = process.argv.slice(2);
    
    if (args.length < 4) {
      console.log('\nUsage: node scripts/create-user.js <email> <password> <name> <role>');
      console.log('Example: node scripts/create-user.js emma.thompson@horizon.edu password123 "Emma Thompson" student\n');
      console.log('Valid roles: student, parent, associate, expert, staff, admin\n');
      process.exit(1);
    }

    const email = args[0];
    const password = args[1];
    const name = args[2];
    const role = args[3];

    const validRoles = ['student', 'parent', 'associate', 'expert', 'staff', 'admin'];
    if (!validRoles.includes(role)) {
      console.error(`\n❌ Invalid role: ${role}`);
      console.log(`Valid roles: ${validRoles.join(', ')}\n`);
      process.exit(1);
    }

    console.log('\n=== Creating User ===\n');
    console.log(`Email: ${email}`);
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
      const nameParts = name.split(' ');
      const firstName = nameParts[0] || 'Student';
      const lastName = nameParts.slice(1).join(' ') || 'User';
      
      const studentResult = await pool.query(
        `INSERT INTO students (user_id, first_name, last_name, grade) 
         VALUES ($1, $2, $3, $4) 
         RETURNING id`,
        [user.id, firstName, lastName, 10]
      );
      console.log(`✅ Student profile created!`);
      console.log(`   Student ID: ${studentResult.rows[0].id}\n`);
    }

    console.log('You can now login with:');
    console.log(`   Email: ${email}`);
    console.log(`   Password: ${password}\n`);

    process.exit(0);
  } catch (error) {
    console.error('Error creating user:', error);
    process.exit(1);
  }
}

createUser();

