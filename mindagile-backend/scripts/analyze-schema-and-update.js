#!/usr/bin/env node

/**
 * Schema Analyzer and App Updater
 * 
 * This script:
 * 1. Reads the database-schema-report.json
 * 2. Analyzes the actual database structure
 * 3. Generates recommendations for updating app queries
 * 4. Creates updated query files
 */

const fs = require('fs');
const path = require('path');

// Read the schema report
const reportPath = path.join(__dirname, '..', 'database-schema-report.json');

if (!fs.existsSync(reportPath)) {
  console.error(`❌ Schema report not found at: ${reportPath}`);
  console.error(`   Please download it from VM first:`);
  console.error(`   scp vkasarla@192.168.100.6:~/Desktop/K-12/backend/database-schema-report.json ./`);
  process.exit(1);
}

const schemaReport = JSON.parse(fs.readFileSync(reportPath, 'utf8'));

console.log(`\n${'='.repeat(80)}`);
console.log(`🔍 ANALYZING DATABASE SCHEMA REPORT`);
console.log(`${'='.repeat(80)}\n`);

// Analyze each database
const analysis = {
  tables: {},
  missingTables: [],
  columnDifferences: {},
  recommendations: []
};

Object.entries(schemaReport).forEach(([dbName, dbSchema]) => {
  if (!dbSchema || !dbSchema.tables) return;

  console.log(`\n📊 Database: ${dbName.toUpperCase()}`);
  console.log(`   ${'-'.repeat(76)}`);

  Object.entries(dbSchema.tables).forEach(([tableName, table]) => {
    if (!analysis.tables[tableName]) {
      analysis.tables[tableName] = {
        exists: table.exists,
        columns: {},
        databases: []
      };
    }

    analysis.tables[tableName].databases.push(dbName);

    if (table.exists) {
      console.log(`\n   ✅ Table: ${tableName} (${table.columns.length} columns, ${table.rowCount} rows)`);
      
      table.columns.forEach(col => {
        if (!analysis.tables[tableName].columns[col.name]) {
          analysis.tables[tableName].columns[col.name] = {
            type: col.type,
            nullable: col.nullable,
            databases: []
          };
        }
        analysis.tables[tableName].columns[col.name].databases.push(dbName);
      });

      // Show key columns
      const keyColumns = table.columns
        .filter(c => c.name.includes('id') || c.name.includes('email') || c.name.includes('name'))
        .map(c => c.name)
        .slice(0, 5);
      if (keyColumns.length > 0) {
        console.log(`      Key columns: ${keyColumns.join(', ')}`);
      }
    } else {
      console.log(`\n   ❌ Table: ${tableName} - DOES NOT EXIST`);
      if (!analysis.missingTables.includes(tableName)) {
        analysis.missingTables.push(tableName);
      }
    }
  });
});

// Generate recommendations
console.log(`\n\n${'='.repeat(80)}`);
console.log(`📋 RECOMMENDATIONS FOR APP UPDATES`);
console.log(`${'='.repeat(80)}\n`);

// Check daily_checkins table
if (analysis.tables.daily_checkins && analysis.tables.daily_checkins.exists) {
  const checkinCols = Object.keys(analysis.tables.daily_checkins.columns);
  const hasEmotion = checkinCols.includes('emotion');
  const hasEmotionIntensity = checkinCols.includes('emotion_intensity');
  
  console.log(`\n1. daily_checkins Table:`);
  console.log(`   ✅ Exists in: ${analysis.tables.daily_checkins.databases.join(', ')}`);
  console.log(`   📝 Available columns: ${checkinCols.join(', ')}`);
  
  if (!hasEmotion) {
    analysis.recommendations.push({
      file: 'routes/studentData.js',
      issue: 'Query tries to select emotion column that does not exist',
      fix: 'Remove emotion from SELECT statement, only use: stress_level, mood_rating, date',
      currentQuery: 'SELECT emotion, stress_level, mood_rating, date FROM daily_checkins',
      recommendedQuery: 'SELECT stress_level, mood_rating, date FROM daily_checkins'
    });
    console.log(`   ⚠️  Column 'emotion' does NOT exist - remove from queries`);
  }
  
  if (!hasEmotionIntensity) {
    console.log(`   ⚠️  Column 'emotion_intensity' does NOT exist - remove from queries`);
  }
}

// Check fhir_observations table
if (analysis.tables.fhir_observations) {
  if (!analysis.tables.fhir_observations.exists) {
    analysis.recommendations.push({
      file: 'routes/studentData.js',
      issue: 'fhir_observations table does not exist',
      fix: 'Keep try/catch block, return empty array if table missing',
      status: '✅ Already handled with try/catch'
    });
    console.log(`\n2. fhir_observations Table:`);
    console.log(`   ❌ Does NOT exist - ensure try/catch returns empty array`);
  }
}

// Check users table columns
if (analysis.tables.users && analysis.tables.users.exists) {
  const userCols = Object.keys(analysis.tables.users.columns);
  const hasFirstName = userCols.includes('first_name');
  const hasLastName = userCols.includes('last_name');
  
  console.log(`\n3. users Table:`);
  console.log(`   ✅ Exists in: ${analysis.tables.users.databases.join(', ')}`);
  console.log(`   📝 Available columns: ${userCols.join(', ')}`);
  
  if (!hasFirstName || !hasLastName) {
    analysis.recommendations.push({
      file: 'routes/communications.js',
      issue: 'Query uses u.first_name and u.last_name which may not exist',
      fix: 'Use COALESCE to handle missing columns',
      currentQuery: 'u.first_name || \' \' || u.last_name as sender_name',
      recommendedQuery: 'COALESCE(NULLIF(TRIM(COALESCE(u.first_name, \'\') || \' \' || COALESCE(u.last_name, \'\')), \'\'), u.email, \'Staff\') as sender_name'
    });
    console.log(`   ⚠️  first_name/last_name may be missing - use COALESCE in queries`);
  }
}

// Check students table
if (analysis.tables.students && analysis.tables.students.exists) {
  const studentCols = Object.keys(analysis.tables.students.columns);
  console.log(`\n4. students Table:`);
  console.log(`   ✅ Exists in: ${analysis.tables.students.databases.join(', ')}`);
  console.log(`   📝 Available columns: ${studentCols.join(', ')}`);
  
  const hasProfilePicture = studentCols.includes('profile_picture_url');
  if (hasProfilePicture) {
    console.log(`   ✅ Has profile_picture_url column - can use in queries`);
  } else {
    console.log(`   ⚠️  No profile_picture_url column - handle gracefully`);
  }
}

// Generate update recommendations file
const recommendationsPath = path.join(__dirname, '..', 'schema-update-recommendations.json');
fs.writeFileSync(recommendationsPath, JSON.stringify(analysis, null, 2));

console.log(`\n\n${'='.repeat(80)}`);
console.log(`💾 ANALYSIS COMPLETE`);
console.log(`${'='.repeat(80)}`);
console.log(`\n📄 Recommendations saved to: ${recommendationsPath}`);
console.log(`\n📋 Summary:`);
console.log(`   - Tables analyzed: ${Object.keys(analysis.tables).length}`);
console.log(`   - Missing tables: ${analysis.missingTables.length}`);
console.log(`   - Recommendations: ${analysis.recommendations.length}`);
console.log(`\n✅ Next steps:`);
console.log(`   1. Review the recommendations`);
console.log(`   2. Update backend routes to match actual schema`);
console.log(`   3. Test with real data from database`);
console.log(`\n`);
