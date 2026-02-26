const express = require('express');
const { authenticateToken, requireRole } = require('../middleware/auth');
const { routeToSchoolDatabase } = require('../middleware/databaseRouter');
const { getPool } = require('../config/databaseManager');

const router = express.Router();

const normalRestriction = {
  system: 'http://terminology.hl7.org/CodeSystem/v3-Confidentiality',
  code: 'N',
  display: 'normal'
};

async function buildFhirExportBundle(schoolPool, studentId, startDate, endDate) {
  const observationsResult = await schoolPool.query(
    `SELECT * FROM fhir_observations
     WHERE student_id = $1
     AND effective_date_time >= $2
     AND effective_date_time <= $3
     ORDER BY effective_date_time DESC`,
    [studentId, startDate, endDate]
  );

  const observations = observationsResult.rows.map(row => ({
    resourceType: 'Observation',
    id: row.observation_id,
    status: row.status,
    meta: { security: [normalRestriction] },
    code: {
      coding: [{
        system: 'http://loinc.org',
        code: row.loinc_code,
        display: row.loinc_display
      }]
    },
    valueQuantity: row.value_quantity ? {
      value: parseFloat(row.value_quantity),
      unit: row.value_unit
    } : null,
    valueString: row.value_string,
    effectiveDateTime: row.effective_date_time,
    subject: {
      reference: row.subject_reference || `Patient/${studentId}`
    }
  }));

  const checkinsResult = await schoolPool.query(
    `SELECT * FROM daily_checkins
     WHERE student_id = $1
     AND date >= $2
     AND date <= $3
     ORDER BY date DESC`,
    [studentId, startDate, endDate]
  );

  const checkinObservations = checkinsResult.rows.map((checkin) => {
    const obsList = [];
    if (checkin.stress_level != null) {
      obsList.push({
        resourceType: 'Observation',
        id: `checkin-stress-${checkin.id}`,
        status: 'final',
        meta: { security: [normalRestriction] },
        code: { coding: [{ system: 'http://loinc.org', code: '76513-1', display: 'Stress level' }] },
        valueQuantity: { value: checkin.stress_level, unit: '1-10 scale' },
        effectiveDateTime: checkin.date + 'T00:00:00Z',
        subject: { reference: `Patient/${studentId}` }
      });
    }
    if (checkin.mood_rating != null) {
      obsList.push({
        resourceType: 'Observation',
        id: `checkin-mood-${checkin.id}`,
        status: 'final',
        meta: { security: [normalRestriction] },
        code: { coding: [{ system: 'http://loinc.org', code: '76536-2', display: 'Mood rating' }] },
        valueQuantity: { value: checkin.mood_rating, unit: '1-5 scale' },
        effectiveDateTime: checkin.date + 'T00:00:00Z',
        subject: { reference: `Patient/${studentId}` }
      });
    }
    return obsList;
  }).flat();

  let activityObservations = [];
  try {
    const activityResult = await schoolPool.query(
      `SELECT id, date, heart_rate, steps, sleep_hours, hydration_percent, nutrition_percent, stress_level
       FROM activity_logs
       WHERE student_id = $1 AND date >= $2 AND date <= $3
       ORDER BY date ASC`,
      [studentId, startDate, endDate]
    );
    activityObservations = activityResult.rows.flatMap(row => {
      const obs = [];
      const effectiveDate = (row.date && typeof row.date === 'object' && row.date.toISOString)
        ? row.date.toISOString().split('T')[0]
        : String(row.date || '').split('T')[0];
      const effectiveDateTime = effectiveDate ? effectiveDate + 'T00:00:00Z' : null;
      if (!effectiveDateTime) return obs;
      if (row.heart_rate != null) {
        obs.push({
          resourceType: 'Observation',
          id: `activity-hr-${row.id}`,
          status: 'final',
          meta: { security: [normalRestriction] },
          code: { coding: [{ system: 'http://loinc.org', code: '8867-4', display: 'Heart rate' }] },
          valueQuantity: { value: row.heart_rate, unit: 'beats/minute' },
          effectiveDateTime,
          subject: { reference: `Patient/${studentId}` }
        });
      }
      if (row.steps != null) {
        obs.push({
          resourceType: 'Observation',
          id: `activity-steps-${row.id}`,
          status: 'final',
          meta: { security: [normalRestriction] },
          code: { coding: [{ system: 'http://loinc.org', code: '41950-7', display: 'Steps' }] },
          valueQuantity: { value: row.steps, unit: 'steps' },
          effectiveDateTime,
          subject: { reference: `Patient/${studentId}` }
        });
      }
      if (row.sleep_hours != null) {
        obs.push({
          resourceType: 'Observation',
          id: `activity-sleep-${row.id}`,
          status: 'final',
          meta: { security: [normalRestriction] },
          code: { coding: [{ system: 'http://loinc.org', code: '93832-4', display: 'Sleep duration' }] },
          valueQuantity: { value: parseFloat(row.sleep_hours), unit: 'h' },
          effectiveDateTime,
          subject: { reference: `Patient/${studentId}` }
        });
      }
      if (row.hydration_percent != null) {
        obs.push({
          resourceType: 'Observation',
          id: `activity-hydration-${row.id}`,
          status: 'final',
          meta: { security: [normalRestriction] },
          code: { coding: [{ system: 'http://loinc.org', code: '9052-2', display: 'Fluid intake' }] },
          valueQuantity: { value: row.hydration_percent, unit: 'percent' },
          effectiveDateTime,
          subject: { reference: `Patient/${studentId}` }
        });
      }
      if (row.stress_level != null) {
        obs.push({
          resourceType: 'Observation',
          id: `activity-stress-${row.id}`,
          status: 'final',
          meta: { security: [normalRestriction] },
          code: { coding: [{ system: 'http://loinc.org', code: '73985-4', display: 'Stress level' }] },
          valueQuantity: { value: row.stress_level, unit: '1-10 scale' },
          effectiveDateTime,
          subject: { reference: `Patient/${studentId}` }
        });
      }
      return obs;
    });
  } catch (e) {
    // activity_logs table may not exist in some DBs
  }

  const allObservations = [...observations, ...checkinObservations, ...activityObservations];
  return {
    resourceType: 'Bundle',
    type: 'collection',
    timestamp: new Date().toISOString(),
    meta: { security: [normalRestriction] },
    entry: allObservations.map(obs => ({ resource: obs }))
  };
}

// Export student data as FHIR R4 Bundle (students: own data only)
router.get('/student-data/fhir-export', authenticateToken, requireRole('student'), routeToSchoolDatabase, async (req, res) => {
  try {
    const { startDate, endDate } = req.query;
    const userId = req.user.userId;
    const schoolName = req.user.schoolName;
    const schoolPool = req.db || (schoolName ? getPool(schoolName) : null);

    if (!schoolPool) {
      return res.status(500).json({ error: 'Database connection error' });
    }

    if (!startDate || !endDate) {
      return res.status(400).json({ error: 'Start date and end date are required' });
    }

    const studentResult = await schoolPool.query(
      'SELECT id FROM students WHERE user_id = $1',
      [userId]
    );

    if (studentResult.rows.length === 0) {
      return res.status(404).json({ error: 'Student profile not found' });
    }

    const studentId = studentResult.rows[0].id;
    const fhirBundle = await buildFhirExportBundle(schoolPool, studentId, startDate, endDate);
    const observationCount = fhirBundle.entry.length;

    res.json({
      success: true,
      fhirBundle: JSON.stringify(fhirBundle, null, 2),
      observationCount
    });
  } catch (error) {
    console.error('FHIR export error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Export a student's data as FHIR R4 Bundle (associates/experts: for a student they have access to)
router.get('/student-data/:studentId/fhir-export', authenticateToken, requireRole('associate', 'expert', 'staff', 'admin'), routeToSchoolDatabase, async (req, res) => {
  try {
    const { studentId } = req.params;
    const { startDate, endDate } = req.query;
    const schoolPool = req.db;

    if (!schoolPool) {
      return res.status(500).json({ error: 'Database connection error' });
    }

    if (!startDate || !endDate) {
      return res.status(400).json({ error: 'Start date and end date are required' });
    }

    const studentRow = await schoolPool.query(
      'SELECT id FROM students WHERE id = $1',
      [studentId]
    );
    if (studentRow.rows.length === 0) {
      return res.status(404).json({ error: 'Student not found' });
    }

    const sid = parseInt(studentId, 10);
    const fhirBundle = await buildFhirExportBundle(schoolPool, sid, startDate, endDate);
    const observationCount = fhirBundle.entry.length;

    res.json({
      success: true,
      fhirBundle: JSON.stringify(fhirBundle, null, 2),
      observationCount
    });
  } catch (error) {
    console.error('FHIR export (associate) error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

module.exports = router;
