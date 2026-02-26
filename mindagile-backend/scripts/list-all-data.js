#!/usr/bin/env node
/**
 * List all data in the backend database(s).
 * Use this to verify data exists and is reflected in the app.
 *
 * K-12 VM layout (Ajay setup):
 *   Project: /home/vkasarla/Desktop/K-12
 *   .env:    /home/vkasarla/Desktop/K-12/.env  (K-12 root)
 *   Backend: /home/vkasarla/Desktop/K-12/backend  (server: backend/src/server.js)
 *   Ports:   /tmp/mindaigle_db_ports.${UID}.env  (written by start-database-containers.sh / START_ALL.sh)
 *   Init:    database/init_school_horizons.sql etc. seed data when containers start.
 *
 * Run from backend dir so ../.env is found:
 *   cd /home/vkasarla/Desktop/K-12/backend && node scripts/list-all-data.js
 *
 * Optional: node scripts/list-all-data.js create-student <email>  [school]
 */

require('dotenv').config();

// Load .env from backend/ and K-12 root (backend/../.env)
let portFileLoaded = null;
try {
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
        portFileLoaded = userPortFile;
      }
    }
    if (!portFileLoaded) {
      const files = fs.readdirSync(tmpDir)
        .filter(f => f.startsWith('mindaigle_db_ports.') && f.endsWith('.env'))
        .sort().reverse();
      if (files.length > 0) {
        const content = fs.readFileSync(path.join(tmpDir, files[0]), 'utf8');
        content.split('\n').forEach(line => {
          const m = line.match(/^\s*([A-Z_0-9]+)=(.+)\s*$/);
          if (m) process.env[m[1]] = m[2].trim();
        });
        portFileLoaded = path.join(tmpDir, files[0]);
      }
    }
  }
} catch (_) { /* ignore */ }

const { Pool } = require('pg');
const useSingleDb = process.env.USE_SINGLE_DB === 'true';
let pool;
let poolsBySchool = null;

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

if (useSingleDb) {
  pool = new Pool({
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5432', 10),
    database: process.env.DB_NAME || 'mindaigle',
    user: process.env.DB_USER || 'postgres',
    password: process.env.DB_PASSWORD || 'postgres',
  });
} else {
  poolsBySchool = {};
  for (const school of ['horizons', 'houghton', 'calumet']) {
    poolsBySchool[school] = new Pool(schoolConfig(school));
  }
  pool = poolsBySchool.horizons || poolsBySchool.houghton || poolsBySchool.calumet;
}

async function listTables(p, tableNames) {
  const out = {};
  for (const name of tableNames) {
    try {
      const r = await p.query(`SELECT COUNT(*) AS c FROM ${name}`);
      out[name] = { count: parseInt(r.rows[0].c, 10) };
    } catch (e) {
      if (e.code === '42P01') out[name] = { count: 0, error: 'table does not exist' };
      else out[name] = { count: 0, error: e.message };
    }
  }
  return out;
}

async function runForPool(p, schoolLabel, tables, createStudentEmail, createStudentSchool) {
  const counts = await listTables(p, tables);
  console.log(`\n--- ${schoolLabel} ---`);
  for (const [name, v] of Object.entries(counts)) {
    const err = v.error ? ` (${v.error})` : '';
    console.log(`  ${name}: ${v.count} rows${err}`);
  }

  console.log(`\n--- ${schoolLabel}: Users (id, email, role) ---`);
  const users = await p.query('SELECT id, email, first_name, last_name, role FROM users ORDER BY id LIMIT 20');
  users.rows.forEach(u => {
    const name = [u.first_name, u.last_name].filter(Boolean).join(' ').trim() || '—';
    console.log(`  ${u.id}  ${u.email}  ${u.role}  ${name}`);
  });
  if (users.rows.length === 0) console.log('  (none)');

  console.log(`\n--- ${schoolLabel}: Students (id, user_id, school_id, name) ---`);
  try {
    const students = await p.query(`
      SELECT s.id, s.user_id, s.school_id, s.first_name, s.last_name, u.email
      FROM students s
      LEFT JOIN users u ON u.id = s.user_id
      ORDER BY s.id LIMIT 20
    `);
    students.rows.forEach(s => {
      const name = [s.first_name, s.last_name].filter(Boolean).join(' ').trim() || '—';
      console.log(`  student_id=${s.id}  user_id=${s.user_id}  school_id=${s.school_id}  ${name}  ${s.email || ''}`);
    });
    if (students.rows.length === 0) {
      console.log('  (none)');
      const studentUsers = await p.query("SELECT email FROM users WHERE role = 'student' LIMIT 5");
      if (studentUsers.rows.length > 0) {
        console.log('  → To fix: node scripts/list-all-data.js create-student <email> ' + (schoolLabel.toLowerCase()) + '  for one of:', studentUsers.rows.map(r => r.email).join(', '));
      }
    }
  } catch (e) {
    console.log('  Error:', e.message);
  }

  console.log(`\n--- ${schoolLabel}: Appointments ---`);
  try {
    const appt = await p.query('SELECT id, student_id, staff_id, appointment_date, status FROM appointments ORDER BY id LIMIT 10');
    appt.rows.forEach(a => console.log(`  ${a.id}  student=${a.student_id}  staff=${a.staff_id}  ${a.appointment_date}  ${a.status}`));
    if (appt.rows.length === 0) console.log('  (none)');
  } catch (e) {
    console.log('  Error:', e.message);
  }

  console.log(`\n--- ${schoolLabel}: Communications (messages) ---`);
  try {
    const comm = await p.query('SELECT id, sender_id, recipient_type, recipient_id, subject, LEFT(message, 40) AS msg, created_at FROM communications ORDER BY id LIMIT 10');
    comm.rows.forEach(c => console.log(`  ${c.id}  from=${c.sender_id}  to=${c.recipient_type}/${c.recipient_id}  ${c.subject || ''}  ${(c.msg || '').substring(0, 40)}`));
    if (comm.rows.length === 0) console.log('  (none)');
  } catch (e) {
    console.log('  Error:', e.message);
  }

  console.log(`\n--- ${schoolLabel}: Screener instances ---`);
  try {
    const si = await p.query('SELECT id, student_id, screener_type, status, completed_at FROM screener_instances ORDER BY id LIMIT 10');
    si.rows.forEach(s => console.log(`  ${s.id}  student=${s.student_id}  ${s.screener_type}  ${s.status}  ${s.completed_at || ''}`));
    if (si.rows.length === 0) console.log('  (none)');
  } catch (e) {
    console.log('  Error:', e.message);
  }

  if (createStudentEmail && (!createStudentSchool || schoolLabel.toLowerCase() === String(createStudentSchool).toLowerCase())) {
    console.log('\n--- Create student profile for:', createStudentEmail, 'in', schoolLabel, '---');
    const u = await p.query('SELECT id, email, first_name, last_name, role FROM users WHERE email = $1', [createStudentEmail]);
    if (u.rows.length === 0) {
      console.log('  User not found in this DB. Create user (signup) first or try another school.');
    } else if (u.rows[0].role !== 'student') {
      console.log('  User role is', u.rows[0].role, '— only student users need a students row.');
    } else {
      const userId = u.rows[0].id;
      const existing = await p.query('SELECT id FROM students WHERE user_id = $1', [userId]);
      if (existing.rows.length > 0) {
        console.log('  Student profile already exists: student_id =', existing.rows[0].id);
      } else {
        let schoolId = 1;
        try {
          let school = await p.query('SELECT id FROM schools LIMIT 1');
          if (school.rows.length === 0) {
            await p.query("INSERT INTO schools (name) VALUES ('Default School') ON CONFLICT (name) DO NOTHING");
            school = await p.query('SELECT id FROM schools LIMIT 1');
          }
          if (school.rows.length > 0) schoolId = school.rows[0].id;
        } catch (_) { /* use 1 */ }
        const first = u.rows[0].first_name || 'Student';
        const last = u.rows[0].last_name || String(userId);
        await p.query(
          `INSERT INTO students (user_id, first_name, last_name, school_id) VALUES ($1, $2, $3, $4)`,
          [userId, first, last, schoolId]
        );
        const inserted = await p.query('SELECT id FROM students WHERE user_id = $1', [userId]);
        console.log('  Created students row: student_id =', inserted.rows[0].id, '| user_id =', userId);
      }
    }
  }
}

async function listAllData() {
  const createStudentEmail = process.argv[2] === 'create-student' ? process.argv[3] : null;
  const createStudentSchool = process.argv[2] === 'create-student' ? (process.argv[4] || 'horizons') : null;

  console.log('\n=== Mindagile DB: list all data ===\n');
  if (portFileLoaded) console.log('Port file:', portFileLoaded);
  if (useSingleDb) {
    console.log('Mode: single DB |', process.env.DB_NAME || 'mindaigle', '| Host:', process.env.DB_HOST || 'localhost', '| Port:', process.env.DB_PORT || '5432');
  } else {
    console.log('Mode: multi-DB (K-12)');
    console.log('  Horizons: port', process.env.DB_PORT_HORIZONS || process.env.HORIZONS_DB_PORT || '5432', '|', process.env.DB_NAME_HORIZONS || process.env.HORIZONS_DB_NAME || 'mindaigle_horizons');
    console.log('  Houghton: port', process.env.DB_PORT_HOUGHTON || process.env.HOUGHTON_DB_PORT || '5433', '|', process.env.DB_NAME_HOUGHTON || process.env.HOUGHTON_DB_NAME || 'mindaigle_houghton');
    console.log('  Calumet: port', process.env.DB_PORT_CALUMET || process.env.CALUMET_DB_PORT || '5434', '|', process.env.DB_NAME_CALUMET || process.env.CALUMET_DB_NAME || 'mindaigle_calumet');
  }
  console.log('');

  try {
    await pool.query('SELECT NOW()');
  } catch (e) {
    console.error('Cannot connect to database:', e.message);
    process.exit(1);
  }

  const tables = ['users', 'students', 'schools', 'appointments', 'communications', 'screener_instances', 'screener_scores', 'daily_checkins', 'activity_logs', 'fhir_observations'];

  if (useSingleDb || !poolsBySchool) {
    await runForPool(pool, 'Default', tables, createStudentEmail, createStudentSchool);
  } else {
    for (const school of ['horizons', 'houghton', 'calumet']) {
      const p = poolsBySchool[school];
      if (p) await runForPool(p, school, tables, createStudentEmail, createStudentSchool);
    }
  }

  console.log('\n=== Done ===\n');
  process.exit(0);
}

listAllData().catch(err => {
  console.error(err);
  process.exit(1);
});
