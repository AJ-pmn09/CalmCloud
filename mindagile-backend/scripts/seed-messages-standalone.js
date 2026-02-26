#!/usr/bin/env node
/**
 * Seed a few test messages (communications) so the Messages screen shows data.
 * Standalone: uses only .env and pg (same loading as list-all-data.js). Safe to run on K-12 VM.
 * Run from backend: node scripts/seed-messages-standalone.js
 */

require('dotenv').config();
const fs = require('fs');
const path = require('path');
const envPaths = [path.join(process.cwd(), '.env'), path.join(process.cwd(), '..', '.env')];
for (const p of envPaths) {
  if (fs.existsSync(p)) {
    const content = fs.readFileSync(p, 'utf8');
    content.split('\n').forEach(line => {
      const m = line.match(/^\s*([A-Z_0-9]+)=(.+)\s*$/);
      if (m && !process.env[m[1]]) process.env[m[1]] = m[2].trim();
    });
  }
}
const tmpDir = '/tmp';
if (fs.existsSync(tmpDir)) {
  const uid = typeof process.getuid === 'function' ? process.getuid() : null;
  if (uid != null) {
    const userPortFile = path.join(tmpDir, `mindaigle_db_ports.${uid}.env`);
    if (fs.existsSync(userPortFile)) {
      const content = fs.readFileSync(userPortFile, 'utf8');
      content.split('\n').forEach(line => {
        const m = line.match(/^\s*([A-Z_0-9]+)=(.+)\s*$/);
        if (m) process.env[m[1]] = m[2].trim();
      });
    }
  }
  if (!process.env.DB_PORT_HORIZONS) {
    const files = fs.readdirSync(tmpDir).filter(f => f.startsWith('mindaigle_db_ports.') && f.endsWith('.env')).sort().reverse();
    if (files.length > 0) {
      const content = fs.readFileSync(path.join(tmpDir, files[0]), 'utf8');
      content.split('\n').forEach(line => {
        const m = line.match(/^\s*([A-Z_0-9]+)=(.+)\s*$/);
        if (m) process.env[m[1]] = m[2].trim();
      });
    }
  }
}

const { Pool } = require('pg');
const useSingleDb = process.env.USE_SINGLE_DB === 'true';

function schoolConfig(school) {
  const u = school.toUpperCase();
  const defaultPort = school === 'horizons' ? '5432' : school === 'houghton' ? '5433' : '5434';
  return {
    host: process.env[`DB_HOST_${u}`] || process.env[`${u}_DB_HOST`] || 'localhost',
    port: parseInt(process.env[`DB_PORT_${u}`] || process.env[`${u}_DB_PORT`] || defaultPort, 10),
    database: process.env[`DB_NAME_${u}`] || process.env[`${u}_DB_NAME`] || `mindaigle_${school}`,
    user: process.env[`DB_USER_${u}`] || process.env[`${u}_DB_USER`] || process.env.DB_USER || 'postgres',
    password: process.env[`DB_PASSWORD_${u}`] || process.env[`${u}_DB_PASSWORD`] || process.env.DB_PASSWORD || 'postgres',
  };
}

async function seedPool(pool, label) {
  try {
    const tableCheck = await pool.query(`
      SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name='communications')
    `);
    if (!tableCheck.rows[0].exists) {
      console.log(`  [${label}] communications table missing - run ensure-tables-all-schools.js first`);
      return 0;
    }
    const associateResult = await pool.query(
      `SELECT id FROM users WHERE role IN ('associate','staff','expert','admin') LIMIT 1`
    );
    const studentResult = await pool.query(`SELECT id FROM students ORDER BY id LIMIT 3`);
    if (associateResult.rows.length === 0 || studentResult.rows.length === 0) {
      console.log(`  [${label}] Skip: no associate or students`);
      return 0;
    }
    const senderId = associateResult.rows[0].id;
    const messages = [
      { subject: 'Welcome', message: 'Hi! This is a test message from staff. Reply anytime.' },
      { subject: 'Check-in reminder', message: 'Hope you are doing well. Reach out if you want to chat.' },
      { subject: 'Quick hello', message: 'Just checking in. Let me know if you need anything.' },
    ];
    let inserted = 0;
    for (let i = 0; i < Math.min(studentResult.rows.length, messages.length); i++) {
      await pool.query(
        `INSERT INTO communications (sender_id, sender_role, recipient_type, recipient_id, subject, message, priority, status)
         VALUES ($1, 'associate', 'student', $2, $3, $4, 'normal', 'sent')`,
        [senderId, studentResult.rows[i].id, messages[i].subject, messages[i].message]
      );
      inserted++;
    }
    console.log(`  [${label}] Inserted ${inserted} test message(s)`);
    return inserted;
  } catch (err) {
    console.error(`  [${label}] Error:`, err.message);
    return 0;
  }
}

async function main() {
  console.log('\nSeeding test messages (staff -> students)...\n');
  let total = 0;
  if (useSingleDb) {
    const pool = new Pool({
      host: process.env.DB_HOST || 'localhost',
      port: parseInt(process.env.DB_PORT || '5432', 10),
      database: process.env.DB_NAME || 'mindaigle',
      user: process.env.DB_USER || 'postgres',
      password: process.env.DB_PASSWORD || 'postgres',
    });
    total += await seedPool(pool, 'default');
    await pool.end();
  } else {
    for (const school of ['horizons', 'houghton', 'calumet']) {
      const pool = new Pool(schoolConfig(school));
      total += await seedPool(pool, school);
      await pool.end();
    }
  }
  console.log('\nDone. Log in as a student in the app and open Messages to see them.\n');
  process.exit(0);
}
main().catch(err => { console.error(err); process.exit(1); });
