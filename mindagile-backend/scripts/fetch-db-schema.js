#!/usr/bin/env node

/**
 * Database Schema Fetcher
 * 
 * This script connects to all school databases and fetches:
 * - Table schemas
 * - Column information
 * - Data types
 * - Constraints
 * - Sample data structure
 * 
 * Run on VM: node scripts/fetch-db-schema.js
 */

const { Pool } = require('pg');
const fs = require('fs');
const path = require('path');

// Database configurations
const databases = {
  horizons: {
    host: process.env.HORIZONS_DB_HOST || 'localhost',
    port: parseInt(process.env.HORIZONS_DB_PORT || '5433'),
    database: process.env.HORIZONS_DB_NAME || 'mindaigle_horizons',
    user: process.env.HORIZONS_DB_USER || process.env.DB_USER || 'postgres',
    password: process.env.HORIZONS_DB_PASSWORD || process.env.DB_PASSWORD || 'postgres',
  },
  houghton: {
    host: process.env.HOUGHTON_DB_HOST || 'localhost',
    port: parseInt(process.env.HOUGHTON_DB_PORT || '5436'),
    database: process.env.HOUGHTON_DB_NAME || 'mindaigle_houghton',
    user: process.env.HOUGHTON_DB_USER || process.env.DB_USER || 'postgres',
    password: process.env.HOUGHTON_DB_PASSWORD || process.env.DB_PASSWORD || 'postgres',
  },
  calumet: {
    host: process.env.CALUMET_DB_HOST || 'localhost',
    port: parseInt(process.env.CALUMET_DB_PORT || '5434'),
    database: process.env.CALUMET_DB_NAME || 'mindaigle_calumet',
    user: process.env.CALUMET_DB_USER || process.env.DB_USER || 'postgres',
    password: process.env.CALUMET_DB_PASSWORD || process.env.DB_PASSWORD || 'postgres',
  }
};

// Tables we're interested in (based on app usage)
const relevantTables = [
  'users',
  'students',
  'daily_checkins',
  'fhir_observations',
  'communications',
  'activity_logs',
  'wearable_data',
  'wellness_scores',
  'symptom_logs',
  'assistance_requests',
  'reminders',
  'emergency_alerts',
  'parents',
  'associates',
  'schools'
];

async function getTableColumns(pool, tableName) {
  try {
    const result = await pool.query(`
      SELECT 
        column_name,
        data_type,
        character_maximum_length,
        is_nullable,
        column_default,
        ordinal_position
      FROM information_schema.columns
      WHERE table_name = $1
      ORDER BY ordinal_position
    `, [tableName]);
    return result.rows;
  } catch (error) {
    return [];
  }
}

async function getTableConstraints(pool, tableName) {
  try {
    const result = await pool.query(`
      SELECT
        tc.constraint_name,
        tc.constraint_type,
        kcu.column_name,
        ccu.table_name AS foreign_table_name,
        ccu.column_name AS foreign_column_name
      FROM information_schema.table_constraints AS tc
      JOIN information_schema.key_column_usage AS kcu
        ON tc.constraint_name = kcu.constraint_name
        AND tc.table_schema = kcu.table_schema
      LEFT JOIN information_schema.constraint_column_usage AS ccu
        ON ccu.constraint_name = tc.constraint_name
        AND ccu.table_schema = tc.table_schema
      WHERE tc.table_name = $1
      ORDER BY tc.constraint_type, tc.constraint_name
    `, [tableName]);
    return result.rows;
  } catch (error) {
    return [];
  }
}

async function getTableIndexes(pool, tableName) {
  try {
    const result = await pool.query(`
      SELECT
        indexname,
        indexdef
      FROM pg_indexes
      WHERE tablename = $1
      ORDER BY indexname
    `, [tableName]);
    return result.rows;
  } catch (error) {
    return [];
  }
}

async function getTableRowCount(pool, tableName) {
  try {
    const result = await pool.query(`SELECT COUNT(*) as count FROM ${tableName}`);
    return parseInt(result.rows[0].count);
  } catch (error) {
    return 0;
  }
}

async function getSampleData(pool, tableName, limit = 1) {
  try {
    const result = await pool.query(`SELECT * FROM ${tableName} LIMIT $1`, [limit]);
    return result.rows;
  } catch (error) {
    return [];
  }
}

async function getAllTables(pool) {
  try {
    const result = await pool.query(`
      SELECT table_name
      FROM information_schema.tables
      WHERE table_schema = 'public'
        AND table_type = 'BASE TABLE'
      ORDER BY table_name
    `);
    return result.rows.map(row => row.table_name);
  } catch (error) {
    return [];
  }
}

async function analyzeDatabase(dbName, config) {
  console.log(`\n${'='.repeat(80)}`);
  console.log(`📊 Analyzing Database: ${dbName.toUpperCase()}`);
  console.log(`   Host: ${config.host}:${config.port}`);
  console.log(`   Database: ${config.database}`);
  console.log(`${'='.repeat(80)}\n`);

  const pool = new Pool(config);
  const schema = {
    database: config.database,
    host: config.host,
    port: config.port,
    tables: {}
  };

  try {
    // Test connection
    await pool.query('SELECT 1');
    console.log(`✅ Connected to ${dbName} database\n`);

    // Get all tables
    const allTables = await getAllTables(pool);
    console.log(`📋 Found ${allTables.length} tables: ${allTables.join(', ')}\n`);

    // Analyze relevant tables
    const tablesToAnalyze = relevantTables.filter(t => allTables.includes(t));
    const missingTables = relevantTables.filter(t => !allTables.includes(t));

    if (missingTables.length > 0) {
      console.log(`⚠️  Missing tables: ${missingTables.join(', ')}\n`);
    }

    for (const tableName of tablesToAnalyze) {
      console.log(`\n📄 Table: ${tableName}`);
      console.log(`   ${'-'.repeat(76)}`);

      // Get columns
      const columns = await getTableColumns(pool, tableName);
      schema.tables[tableName] = {
        exists: true,
        columns: columns.map(col => ({
          name: col.column_name,
          type: col.data_type,
          maxLength: col.character_maximum_length,
          nullable: col.is_nullable === 'YES',
          default: col.column_default,
          position: col.ordinal_position
        })),
        constraints: [],
        indexes: [],
        rowCount: 0,
        sampleData: []
      };

      console.log(`   Columns (${columns.length}):`);
      columns.forEach(col => {
        const nullable = col.is_nullable === 'YES' ? 'NULL' : 'NOT NULL';
        const defaultVal = col.column_default ? ` DEFAULT ${col.column_default}` : '';
        const length = col.character_maximum_length ? `(${col.character_maximum_length})` : '';
        console.log(`     - ${col.column_name}: ${col.data_type}${length} ${nullable}${defaultVal}`);
      });

      // Get constraints
      const constraints = await getTableConstraints(pool, tableName);
      if (constraints.length > 0) {
        schema.tables[tableName].constraints = constraints.map(c => ({
          name: c.constraint_name,
          type: c.constraint_type,
          column: c.column_name,
          foreignTable: c.foreign_table_name,
          foreignColumn: c.foreign_column_name
        }));
        console.log(`   Constraints (${constraints.length}):`);
        constraints.forEach(c => {
          if (c.constraint_type === 'FOREIGN KEY') {
            console.log(`     - ${c.constraint_name}: ${c.constraint_type} (${c.column_name} -> ${c.foreign_table_name}.${c.foreign_column_name})`);
          } else {
            console.log(`     - ${c.constraint_name}: ${c.constraint_type} (${c.column_name})`);
          }
        });
      }

      // Get indexes
      const indexes = await getTableIndexes(pool, tableName);
      if (indexes.length > 0) {
        schema.tables[tableName].indexes = indexes.map(i => ({
          name: i.indexname,
          definition: i.indexdef
        }));
        console.log(`   Indexes (${indexes.length}):`);
        indexes.forEach(i => {
          console.log(`     - ${i.indexname}`);
        });
      }

      // Get row count
      const rowCount = await getTableRowCount(pool, tableName);
      schema.tables[tableName].rowCount = rowCount;
      console.log(`   Row Count: ${rowCount}`);

      // Get sample data (1 row)
      if (rowCount > 0) {
        const sample = await getSampleData(pool, tableName, 1);
        if (sample.length > 0) {
          schema.tables[tableName].sampleData = [sample[0]];
          console.log(`   Sample Data Keys: ${Object.keys(sample[0]).join(', ')}`);
        }
      }
    }

    // Mark missing tables
    for (const tableName of missingTables) {
      schema.tables[tableName] = {
        exists: false,
        columns: [],
        constraints: [],
        indexes: [],
        rowCount: 0,
        sampleData: []
      };
    }

    await pool.end();
    return schema;

  } catch (error) {
    console.error(`❌ Error analyzing ${dbName} database:`, error.message);
    await pool.end();
    return null;
  }
}

async function generateComparisonReport(allSchemas) {
  console.log(`\n${'='.repeat(80)}`);
  console.log(`📊 DATABASE SCHEMA COMPARISON REPORT`);
  console.log(`${'='.repeat(80)}\n`);

  // Compare tables across databases
  const allTableNames = new Set();
  Object.values(allSchemas).forEach(schema => {
    if (schema && schema.tables) {
      Object.keys(schema.tables).forEach(table => allTableNames.add(table));
    }
  });

  console.log(`📋 Tables Comparison:\n`);
  for (const tableName of Array.from(allTableNames).sort()) {
    console.log(`\nTable: ${tableName}`);
    console.log(`   ${'-'.repeat(76)}`);
    
    Object.entries(allSchemas).forEach(([dbName, schema]) => {
      if (!schema || !schema.tables[tableName]) {
        console.log(`   ${dbName.padEnd(12)}: ❌ Table does not exist`);
        return;
      }

      const table = schema.tables[tableName];
      if (!table.exists) {
        console.log(`   ${dbName.padEnd(12)}: ⚠️  Table missing`);
        return;
      }

      const columnNames = table.columns.map(c => c.name).join(', ');
      console.log(`   ${dbName.padEnd(12)}: ✅ ${table.columns.length} columns, ${table.rowCount} rows`);
      console.log(`              Columns: ${columnNames}`);
    });
  }

  // Column differences
  console.log(`\n\n🔍 Column Differences:\n`);
  for (const tableName of Array.from(allTableNames).sort()) {
    const schemasWithTable = Object.entries(allSchemas)
      .filter(([_, schema]) => schema && schema.tables[tableName] && schema.tables[tableName].exists)
      .map(([name, schema]) => ({ name, table: schema.tables[tableName] }));

    if (schemasWithTable.length < 2) continue;

    const allColumns = new Set();
    schemasWithTable.forEach(({ table }) => {
      table.columns.forEach(col => allColumns.add(col.name));
    });

    const columnDifferences = [];
    for (const colName of allColumns) {
      const columnInfo = schemasWithTable.map(({ name, table }) => {
        const col = table.columns.find(c => c.name === colName);
        return { db: name, exists: !!col, type: col?.type, nullable: col?.nullable };
      });

      const existsInAll = columnInfo.every(ci => ci.exists);
      const existsInSome = columnInfo.some(ci => ci.exists);

      if (!existsInAll) {
        columnDifferences.push({
          table: tableName,
          column: colName,
          info: columnInfo
        });
      }
    }

    if (columnDifferences.length > 0) {
      console.log(`\n   Table: ${tableName}`);
      columnDifferences.forEach(diff => {
        const status = diff.info.map(i => 
          i.exists ? `✅ ${i.db}` : `❌ ${i.db}`
        ).join(' | ');
        console.log(`     - ${diff.column}: ${status}`);
      });
    }
  }
}

async function main() {
  console.log(`\n${'='.repeat(80)}`);
  console.log(`🔍 DATABASE SCHEMA FETCHER`);
  console.log(`   Fetching schema information from all school databases`);
  console.log(`${'='.repeat(80)}\n`);

  const allSchemas = {};

  // Analyze each database
  for (const [dbName, config] of Object.entries(databases)) {
    const schema = await analyzeDatabase(dbName, config);
    if (schema) {
      allSchemas[dbName] = schema;
    }
  }

  // Generate comparison report
  if (Object.keys(allSchemas).length > 0) {
    generateComparisonReport(allSchemas);

    // Save to JSON file
    const outputPath = path.join(__dirname, '..', 'database-schema-report.json');
    fs.writeFileSync(outputPath, JSON.stringify(allSchemas, null, 2));
    console.log(`\n\n💾 Full schema report saved to: ${outputPath}`);

    // Save human-readable report
    const readablePath = path.join(__dirname, '..', 'database-schema-report.txt');
    const readableReport = generateReadableReport(allSchemas);
    fs.writeFileSync(readablePath, readableReport);
    console.log(`📄 Human-readable report saved to: ${readablePath}`);

    console.log(`\n✅ Schema analysis complete!\n`);
  } else {
    console.log(`\n❌ Failed to analyze any databases\n`);
    process.exit(1);
  }
}

function generateReadableReport(allSchemas) {
  let report = `DATABASE SCHEMA REPORT\n`;
  report += `Generated: ${new Date().toISOString()}\n`;
  report += `${'='.repeat(80)}\n\n`;

  Object.entries(allSchemas).forEach(([dbName, schema]) => {
    if (!schema) return;

    report += `\n${'='.repeat(80)}\n`;
    report += `DATABASE: ${dbName.toUpperCase()}\n`;
    report += `Host: ${schema.host}:${schema.port}\n`;
    report += `Database: ${schema.database}\n`;
    report += `${'='.repeat(80)}\n\n`;

    Object.entries(schema.tables).forEach(([tableName, table]) => {
      if (!table.exists) {
        report += `\nTable: ${tableName} - ❌ DOES NOT EXIST\n`;
        return;
      }

      report += `\nTable: ${tableName}\n`;
      report += `${'-'.repeat(80)}\n`;
      report += `Row Count: ${table.rowCount}\n`;
      report += `Columns (${table.columns.length}):\n`;

      table.columns.forEach(col => {
        const nullable = col.nullable ? 'NULL' : 'NOT NULL';
        const defaultVal = col.default ? ` DEFAULT ${col.default}` : '';
        const length = col.maxLength ? `(${col.maxLength})` : '';
        report += `  - ${col.name.padEnd(30)} ${col.type}${length} ${nullable}${defaultVal}\n`;
      });

      if (table.constraints.length > 0) {
        report += `\nConstraints:\n`;
        table.constraints.forEach(c => {
          if (c.type === 'FOREIGN KEY') {
            report += `  - ${c.name}: ${c.type} (${c.column} -> ${c.foreignTable}.${c.foreignColumn})\n`;
          } else {
            report += `  - ${c.name}: ${c.type} (${c.column})\n`;
          }
        });
      }

      if (table.sampleData.length > 0) {
        report += `\nSample Data (1 row):\n`;
        Object.entries(table.sampleData[0]).forEach(([key, value]) => {
          const valStr = value === null ? 'NULL' : String(value);
          report += `  ${key}: ${valStr}\n`;
        });
      }

      report += `\n`;
    });
  });

  return report;
}

// Run the script
main().catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});
