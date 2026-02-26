const { Pool } = require('pg');
require('dotenv').config();

/**
 * Multi-Database Manager
 * Single-DB mode: set USE_SINGLE_DB=true so all schools use one DB (DB_HOST, DB_PORT, DB_NAME).
 * Multi-DB mode: set USE_SINGLE_DB=false or leave unset; set per-school vars (HORIZONS_DB_*, HOUGHTON_DB_*, CALUMET_DB_*).
 * Auth (config/database.js) always uses DB_HOST/DB_PORT/DB_NAME for login lookups; can be same as one school or a central DB.
 */

const useSingleDb = process.env.USE_SINGLE_DB === 'true';

const singleDbConfig = {
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '5432'),
  database: process.env.DB_NAME || 'mindaigle',
  user: process.env.DB_USER || 'postgres',
  password: process.env.DB_PASSWORD || 'postgres',
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 10000,
};

// Helper: read per-school config supporting both playbook and .env naming
// Playbook (start-backend-host.sh): DB_HOST_HORIZONS, DB_PORT_HORIZONS, DB_NAME_HORIZONS, DB_USER_HORIZONS, DB_PASSWORD_HORIZONS
// .env style: HORIZONS_DB_HOST, HORIZONS_DB_PORT, HORIZONS_DB_NAME, HORIZONS_DB_USER, HORIZONS_DB_PASSWORD
function schoolConfig(school) {
  const u = school.toUpperCase();
  return {
    host: process.env[`DB_HOST_${u}`] || process.env[`${u}_DB_HOST`] || 'localhost',
    port: parseInt(process.env[`DB_PORT_${u}`] || process.env[`${u}_DB_PORT`] || (school === 'horizons' ? '5433' : school === 'houghton' ? '5436' : '5434')),
    database: process.env[`DB_NAME_${u}`] || process.env[`${u}_DB_NAME`] || `mindaigle_${school}`,
    user: process.env[`DB_USER_${u}`] || process.env[`${u}_DB_USER`] || process.env.DB_USER || 'postgres',
    password: process.env[`DB_PASSWORD_${u}`] || process.env[`${u}_DB_PASSWORD`] || process.env.DB_PASSWORD || 'postgres',
    max: 20,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 10000,
  };
}

// School database configurations
// When USE_SINGLE_DB is true, all three schools use singleDbConfig (DB_HOST, DB_PORT, DB_NAME).
// When USE_SINGLE_DB is false/unset, each school uses its own config from env (HORIZONS_DB_*, etc.).
const schoolDatabases = useSingleDb
  ? {
      'horizons': { ...singleDbConfig },
      'houghton': { ...singleDbConfig },
      'calumet': { ...singleDbConfig },
    }
  : {
  'horizons': schoolConfig('horizons'),
  'houghton': schoolConfig('houghton'),
  'calumet': schoolConfig('calumet'),
};

// Map school_id to school name (for routing when user is in master DB with school_id)
// JWT schoolName is preferred; this mapping is used when login finds user in master DB
const schoolIdToName = {
  1: 'horizons',
  7: 'horizons',
  8: 'houghton',
  9: 'calumet',
};

// Connection pools for each school
const pools = {};

if (useSingleDb) {
  console.log(`📌 Single-DB mode: all schools use database "${singleDbConfig.database}" (port ${singleDbConfig.port})`);
}

// Initialize connection pools for each school
if (useSingleDb) {
  const singlePool = new Pool(singleDbConfig);
  singlePool.on('error', (err) => console.error('❌ Database pool error:', err));
  singlePool.query('SELECT NOW()', (err, res) => {
    if (err) console.error('❌ Database connection error:', err.message);
    else console.log(`✅ Database connected: mindaigle (port ${singleDbConfig.port})`);
  });
  pools['horizons'] = singlePool;
  pools['houghton'] = singlePool;
  pools['calumet'] = singlePool;
} else {
  console.log(`📌 Multi-DB mode: horizons, houghton, calumet each use their own database`);
  Object.keys(schoolDatabases).forEach(school => {
    try {
      const cfg = schoolDatabases[school];
      pools[school] = new Pool(cfg);
      pools[school].on('error', (err) => console.error(`❌ Database pool error for ${school}:`, err));
      pools[school].query('SELECT NOW()', (err, res) => {
        if (err) console.error(`❌ Database connection error for ${school}:`, err.message);
        else console.log(`✅ Database connected: ${school} → ${cfg.database} (port ${cfg.port})`);
      });
    } catch (error) {
      console.error(`❌ Failed to create pool for ${school}:`, error);
    }
  });
}

/**
 * Get database pool for a school by name
 * @param {string} schoolName - 'horizons', 'houghton', or 'calumet'
 * @returns {Pool} PostgreSQL connection pool
 */
function getPool(schoolName) {
  const normalized = schoolName.toLowerCase();
  if (!pools[normalized]) {
    throw new Error(`Unknown school: ${schoolName}. Available: ${Object.keys(pools).join(', ')}`);
  }
  return pools[normalized];
}

/**
 * Get database pool for a school by school_id
 * @param {number} schoolId - School ID (7, 8, or 9)
 * @returns {Pool} PostgreSQL connection pool
 */
function getPoolBySchoolId(schoolId) {
  const schoolName = schoolIdToName[schoolId];
  if (!schoolName) {
    throw new Error(`Unknown school_id: ${schoolId}. Available: ${Object.keys(schoolIdToName).join(', ')}`);
  }
  return getPool(schoolName);
}

/**
 * Get school name from school_id
 * @param {number} schoolId - School ID
 * @returns {string} School name
 */
function getSchoolName(schoolId) {
  return schoolIdToName[schoolId] || null;
}

/**
 * Get all available pools (for health checks)
 * @returns {Object} All connection pools
 */
function getAllPools() {
  return pools;
}

/**
 * Test all database connections
 * @returns {Promise<Object>} Connection status for each school
 */
async function testAllConnections() {
  const results = {};
  
  for (const [school, pool] of Object.entries(pools)) {
    try {
      const result = await pool.query('SELECT NOW()');
      results[school] = {
        status: 'connected',
        timestamp: result.rows[0].now
      };
    } catch (error) {
      results[school] = {
        status: 'error',
        error: error.message
      };
    }
  }
  
  return results;
}

// Graceful shutdown
process.on('SIGINT', async () => {
  console.log('\n🛑 Closing database connections...');
  const seen = new Set();
  for (const [school, pool] of Object.entries(pools)) {
    if (seen.has(pool)) continue;
    seen.add(pool);
    try {
      await pool.end();
      console.log(`✅ Closed ${useSingleDb ? 'mindaigle' : school} database connection`);
    } catch (error) {
      console.error(`❌ Error closing connection:`, error);
    }
  }
  process.exit(0);
});

module.exports = {
  getPool,
  getPoolBySchoolId,
  getSchoolName,
  getAllPools,
  testAllConnections,
  schoolIdToName
};

