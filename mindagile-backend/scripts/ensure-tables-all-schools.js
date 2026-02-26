#!/usr/bin/env node
/**
 * Create missing tables (communications, appointments, screener_*, notifications) in each school DB.
 * Standalone: uses only .env and pg. Run from backend dir: node scripts/ensure-tables-all-schools.js
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
  const files = fs.readdirSync(tmpDir).filter(f => f.startsWith('mindaigle_db_ports.') && f.endsWith('.env')).sort().reverse();
  if (files.length > 0 && !process.env.DB_PORT_HORIZONS) {
    const content = fs.readFileSync(path.join(tmpDir, files[0]), 'utf8');
    content.split('\n').forEach(line => {
      const m = line.match(/^\s*([A-Z_0-9]+)=(.+)\s*$/);
      if (m) process.env[m[1]] = m[2].trim();
    });
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

const sql = [
  `CREATE TABLE IF NOT EXISTS appointments (
    id SERIAL PRIMARY KEY,
    student_id INTEGER NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    staff_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    parent_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    appointment_date TIMESTAMP NOT NULL,
    duration INTEGER DEFAULT 30,
    type VARCHAR(100) NOT NULL DEFAULT 'general',
    notes TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'confirmed', 'completed', 'cancelled')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by INTEGER REFERENCES users(id) ON DELETE SET NULL
  )`,
  `CREATE TABLE IF NOT EXISTS communications (
    id SERIAL PRIMARY KEY,
    sender_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sender_role VARCHAR(50) NOT NULL,
    recipient_type VARCHAR(50) NOT NULL CHECK (recipient_type IN ('student', 'cohort', 'all')),
    recipient_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    recipient_cohort VARCHAR(255),
    subject VARCHAR(255),
    message TEXT NOT NULL,
    priority VARCHAR(50) DEFAULT 'normal' CHECK (priority IN ('low', 'normal', 'high', 'urgent')),
    parent_visible BOOLEAN DEFAULT true,
    emergency_override BOOLEAN DEFAULT false,
    status VARCHAR(50) DEFAULT 'sent' CHECK (status IN ('draft', 'sent', 'read', 'archived')),
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  )`,
  `CREATE INDEX IF NOT EXISTS idx_communications_recipient ON communications(recipient_type, recipient_id, recipient_cohort)`,
  `CREATE INDEX IF NOT EXISTS idx_communications_sender ON communications(sender_id, created_at)`,
  `CREATE TABLE IF NOT EXISTS screener_instances (
    id SERIAL PRIMARY KEY,
    student_id INTEGER NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    screener_type VARCHAR(50) NOT NULL CHECK (screener_type IN ('phq9', 'gad7')),
    status VARCHAR(50) DEFAULT 'assigned' CHECK (status IN ('assigned', 'in_progress', 'completed', 'expired')),
    trigger_source VARCHAR(50) DEFAULT 'manual' CHECK (trigger_source IN ('manual', 'scheduled', 'trigger')),
    assigned_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  )`,
  `CREATE TABLE IF NOT EXISTS screener_responses (
    id SERIAL PRIMARY KEY,
    instance_id INTEGER NOT NULL REFERENCES screener_instances(id) ON DELETE CASCADE,
    question_index INTEGER NOT NULL CHECK (question_index >= 0),
    answer_value INTEGER NOT NULL CHECK (answer_value >= 0 AND answer_value <= 3),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(instance_id, question_index)
  )`,
  `CREATE TABLE IF NOT EXISTS screener_scores (
    id SERIAL PRIMARY KEY,
    instance_id INTEGER NOT NULL UNIQUE REFERENCES screener_instances(id) ON DELETE CASCADE,
    total INTEGER NOT NULL,
    severity VARCHAR(50) NOT NULL,
    positive BOOLEAN NOT NULL DEFAULT false,
    details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  )`,
  `CREATE TABLE IF NOT EXISTS notifications (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    related_type VARCHAR(100),
    related_id INTEGER,
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  )`,
];

async function run() {
  const schools = useSingleDb ? ['default'] : ['horizons', 'houghton', 'calumet'];
  for (const school of schools) {
    const pool = useSingleDb
      ? new Pool({
          host: process.env.DB_HOST || 'localhost',
          port: parseInt(process.env.DB_PORT || '5432', 10),
          database: process.env.DB_NAME || 'mindaigle',
          user: process.env.DB_USER || 'postgres',
          password: process.env.DB_PASSWORD || 'postgres',
        })
      : new Pool(schoolConfig(school));
    const label = useSingleDb ? 'default' : school;
    console.log('\n---', label, '---');
    for (let i = 0; i < sql.length; i++) {
      try {
        await pool.query(sql[i]);
        const match = sql[i].match(/CREATE TABLE IF NOT EXISTS (\w+)/) || sql[i].match(/CREATE INDEX IF NOT EXISTS (\w+)/);
        console.log('  OK', match ? match[1] : 'statement ' + (i + 1));
      } catch (e) {
        if (e.code === '42P07') console.log('  (already exists)', sql[i].match(/TABLE IF NOT EXISTS (\w+)|INDEX IF NOT EXISTS (\w+)/)?.[1] || '');
        else console.log('  Error:', e.message);
      }
    }
    await pool.end();
  }
  console.log('\nDone.\n');
  process.exit(0);
}
run().catch(err => { console.error(err); process.exit(1); });
