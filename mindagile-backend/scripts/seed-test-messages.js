#!/usr/bin/env node
/**
 * Seed test messages (communications) for testing the Messages screen.
 * Run from mindagile-backend: node scripts/seed-test-messages.js
 * Inserts a few messages from staff to students so "Total Messages Sent" and the message list show data.
 * Seeds both the default DB (config/database) and each school DB (databaseManager) so data shows regardless of backend config.
 */

const poolDefault = require('../config/database');
const { getPool } = require('../config/databaseManager');

const SCHOOL_NAMES = ['horizons', 'houghton', 'calumet'];

async function seedPool(pool, label) {
  try {
    const associateResult = await pool.query(
      `SELECT id FROM users WHERE role IN ('associate','staff','expert','admin') LIMIT 1`
    );
    const studentResult = await pool.query(
      `SELECT id FROM students ORDER BY id LIMIT 3`
    );
    if (associateResult.rows.length === 0 || studentResult.rows.length === 0) {
      console.log(`  [${label}] Skip: no associate or students in this DB`);
      return 0;
    }
    const senderId = associateResult.rows[0].id;
    const students = studentResult.rows;
    const messages = [
      { subject: 'Welcome', message: 'Hi! This is a test message from staff. Reply anytime.' },
      { subject: 'Check-in reminder', message: 'Hope you are doing well. Reach out if you want to chat.' },
      { subject: 'Quick hello', message: 'Just checking in. Let me know if you need anything.' },
    ];
    let inserted = 0;
    for (let i = 0; i < Math.min(students.length, messages.length); i++) {
      await pool.query(
        `INSERT INTO communications (sender_id, sender_role, recipient_type, recipient_id, subject, message, priority, status)
         VALUES ($1, 'associate', 'student', $2, $3, $4, 'normal', 'sent')`,
        [senderId, students[i].id, messages[i].subject, messages[i].message]
      );
      inserted++;
    }
    console.log(`  [${label}] Inserted ${inserted} test message(s)`);
    return inserted;
  } catch (err) {
    if (err.code === '42P01') {
      console.log(`  [${label}] Skip: communications table does not exist (backend will create it on first use)`);
    } else {
      console.error(`  [${label}] Error:`, err.message);
    }
    return 0;
  }
}

async function main() {
  console.log('Seeding test messages (staff -> students)...');
  let total = 0;
  try {
    total += await seedPool(poolDefault, 'default DB');
  } catch (e) {
    console.log('  [default DB] Not used or error:', e.message);
  }
  for (const school of SCHOOL_NAMES) {
    const pool = getPool(school);
    if (pool) total += await seedPool(pool, school);
  }
  console.log('Done. Open the app Messages tab (as associate/staff) to see test messages.');
  if (total === 0) {
    console.log('Tip: Ensure backend has run at least once so communications table exists, and DB has users + students.');
  }
}

main().catch(console.error);
