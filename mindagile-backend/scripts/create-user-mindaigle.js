/**
 * Create user in mindaigle (matches schema: first_name, last_name).
 * Use this when login fails with "Invalid email or password" - ensures user exists in the DB.
 *
 * Usage: node scripts/create-user-mindaigle.js <email> <password> <first_name> <last_name> <role>
 * Example: node scripts/create-user-mindaigle.js emma.thompson@horizon.edu MyPassword123 Emma Thompson student
 */

const pool = require('../config/database');
const bcrypt = require('bcrypt');

async function createUser() {
  try {
    const args = process.argv.slice(2);
    if (args.length < 5) {
      console.log('\nUsage: node scripts/create-user-mindaigle.js <email> <password> <first_name> <last_name> <role>');
      console.log('Example: node scripts/create-user-mindaigle.js emma.thompson@horizon.edu MyPass123 Emma Thompson student\n');
      console.log('Valid roles: student, parent, associate, expert, staff, admin\n');
      process.exit(1);
    }

    const [email, password, firstName, lastName, role] = args;
    const validRoles = ['student', 'parent', 'associate', 'expert', 'staff', 'admin'];
    if (!validRoles.includes(role)) {
      console.error('\nInvalid role:', role);
      process.exit(1);
    }

    const existing = await pool.query('SELECT id, email FROM users WHERE email = $1', [email]);
    if (existing.rows.length > 0) {
      console.log('\nUser already exists. Resetting password...');
      const passwordHash = await bcrypt.hash(password, 10);
      await pool.query('UPDATE users SET password_hash = $1, first_name = $2, last_name = $3 WHERE email = $4',
        [passwordHash, firstName, lastName, email]);
      console.log('Password updated. You can login with:', email, '\n');
      process.exit(0);
    }

    const passwordHash = await bcrypt.hash(password, 10);
    const result = await pool.query(
      `INSERT INTO users (email, password_hash, first_name, last_name, role)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING id, email, first_name, last_name, role`,
      [email, passwordHash, firstName, lastName, role]
    );
    const user = result.rows[0];

    if (role === 'student') {
      const existingStudent = await pool.query('SELECT id FROM students WHERE user_id = $1', [user.id]);
      if (existingStudent.rows.length === 0) {
        await pool.query(
          `INSERT INTO students (user_id, first_name, last_name, grade, school_id)
           VALUES ($1, $2, $3, 10, 1)`,
          [user.id, firstName, lastName]
        );
      }
    }

    console.log('\nUser created. You can login with:');
    console.log('  Email:', email);
    console.log('  Password:', password);
    console.log('');
    process.exit(0);
  } catch (error) {
    console.error('Error:', error.message);
    process.exit(1);
  }
}

createUser();
