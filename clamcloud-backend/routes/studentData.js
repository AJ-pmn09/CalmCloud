const express = require('express');
const pool = require('../config/database');
const { authenticateToken, requireRole } = require('../middleware/auth');

const router = express.Router();

// Helper function to create FHIR observation
function createFHIRObservation(studentId, loincCode, loincDisplay, value, valueType, unit = null) {
  const observationId = `${loincCode.toLowerCase()}-${studentId}-${Date.now()}`;
  return {
    observation_id: observationId,
    student_id: studentId,
    resource_type: 'Observation',
    status: 'final',
    loinc_code: loincCode,
    loinc_display: loincDisplay,
    value_quantity: valueType === 'quantity' ? value : null,
    value_string: valueType === 'string' ? value : null,
    value_unit: unit,
    effective_date_time: new Date().toISOString(),
    subject_reference: `Patient/${studentId}`
  };
}

// Save student FHIR health data
router.post('/student-data', authenticateToken, async (req, res) => {
  try {
    const { studentId, fhirData } = req.body;
    const userId = req.user.userId;
    const userRole = req.user.role;

    // Determine target student ID
    let targetStudentId = studentId;
    if (userRole === 'student') {
      // Students can only save their own data
      const studentResult = await pool.query(
        'SELECT id FROM students WHERE user_id = $1',
        [userId]
      );
      if (studentResult.rows.length === 0) {
        return res.status(404).json({ error: 'Student profile not found' });
      }
      targetStudentId = studentResult.rows[0].id;
    } else if (!targetStudentId) {
      return res.status(400).json({ error: 'studentId required' });
    }

    // Verify student exists
    const studentCheck = await pool.query(
      'SELECT id FROM students WHERE id = $1',
      [targetStudentId]
    );
    if (studentCheck.rows.length === 0) {
      return res.status(404).json({ error: 'Student not found' });
    }

    // Save observations
    if (fhirData.observations && Array.isArray(fhirData.observations)) {
      for (const obs of fhirData.observations) {
        await pool.query(
          `INSERT INTO fhir_observations 
           (observation_id, student_id, resource_type, status, loinc_code, loinc_display, 
            value_quantity, value_string, value_unit, effective_date_time, subject_reference)
           VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
           ON CONFLICT (observation_id) DO UPDATE SET
           value_quantity = EXCLUDED.value_quantity,
           value_string = EXCLUDED.value_string,
           updated_at = CURRENT_TIMESTAMP`,
          [
            obs.id || obs.observation_id,
            targetStudentId,
            obs.resourceType || 'Observation',
            obs.status || 'final',
            obs.code?.coding?.[0]?.code || obs.loinc_code,
            obs.code?.coding?.[0]?.display || obs.loinc_display,
            obs.valueQuantity?.value || obs.value_quantity,
            obs.valueString || obs.value_string,
            obs.valueQuantity?.unit || obs.value_unit,
            obs.effectiveDateTime || new Date().toISOString(),
            obs.subject?.reference || `Patient/${targetStudentId}`
          ]
        );
      }
    }

    // Also save to daily_checkins for quick access
    if (fhirData.emotion || fhirData.stressLevel) {
      const emotionObs = fhirData.observations?.find(o => 
        o.loinc_code === '75258-2' || o.code?.coding?.[0]?.code === '75258-2'
      );
      const stressObs = fhirData.observations?.find(o => 
        o.loinc_code === '73985-4' || o.code?.coding?.[0]?.code === '73985-4'
      );

      await pool.query(
        `INSERT INTO daily_checkins 
         (student_id, date, emotion, emotion_intensity, stress_level)
         VALUES ($1, CURRENT_DATE, $2, $3, $4)
         ON CONFLICT (student_id, date) DO UPDATE SET
         emotion = EXCLUDED.emotion,
         emotion_intensity = EXCLUDED.emotion_intensity,
         stress_level = EXCLUDED.stress_level`,
        [
          targetStudentId,
          emotionObs?.value_string || fhirData.emotion,
          emotionObs?.value_quantity || fhirData.emotionIntensity,
          stressObs?.value_quantity || fhirData.stressLevel
        ]
      );
    }

    res.json({
      success: true,
      data: fhirData
    });
  } catch (error) {
    console.error('Save student data error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Get student FHIR data
router.get('/student-data/:studentId', authenticateToken, async (req, res) => {
  try {
    const { studentId } = req.params;
    const userId = req.user.userId;
    const userRole = req.user.role;

    // Permission check
    if (userRole === 'student') {
      const studentResult = await pool.query(
        'SELECT id FROM students WHERE user_id = $1 AND id = $2',
        [userId, studentId]
      );
      if (studentResult.rows.length === 0) {
        return res.status(403).json({ error: 'Access denied' });
      }
    } else if (userRole === 'parent') {
      const linkResult = await pool.query(
        'SELECT id FROM parent_child_links WHERE parent_id = $1 AND student_id = $2 AND status = $3',
        [userId, studentId, 'active']
      );
      if (linkResult.rows.length === 0) {
        return res.status(403).json({ error: 'Access denied - not linked to this student' });
      }
    }

    // Get student info
    const studentResult = await pool.query(
      `SELECT s.*, u.email, u.name as user_name 
       FROM students s 
       JOIN users u ON s.user_id = u.id 
       WHERE s.id = $1`,
      [studentId]
    );

    if (studentResult.rows.length === 0) {
      return res.status(404).json({ error: 'Student not found' });
    }

    const student = studentResult.rows[0];

    // Get FHIR observations
    const observationsResult = await pool.query(
      `SELECT * FROM fhir_observations 
       WHERE student_id = $1 
       ORDER BY effective_date_time DESC 
       LIMIT 100`,
      [studentId]
    );

    const observations = observationsResult.rows.map(row => ({
      id: row.observation_id,
      resourceType: row.resource_type,
      status: row.status,
      code: {
        coding: [{
          system: 'http://loinc.org',
          code: row.loinc_code,
          display: row.loinc_display
        }]
      },
      valueQuantity: row.value_quantity ? {
        value: row.value_quantity,
        unit: row.value_unit
      } : undefined,
      valueString: row.value_string,
      effectiveDateTime: row.effective_date_time,
      subject: {
        reference: row.subject_reference
      }
    }));

    res.json({
      student: {
        id: student.id,
        name: `${student.first_name} ${student.last_name}`,
        email: student.email
      },
      fhirData: {
        observations
      },
      isParentView: userRole === 'parent'
    });
  } catch (error) {
    console.error('Get student data error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Get all students (associates/experts only)
router.get('/students', authenticateToken, requireRole('associate', 'expert'), async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT s.*, u.email, u.name as user_name,
       (SELECT COUNT(*) FROM fhir_observations WHERE student_id = s.id) as observation_count,
       (SELECT MAX(effective_date_time) FROM fhir_observations WHERE student_id = s.id) as last_checkin
       FROM students s
       JOIN users u ON s.user_id = u.id
       ORDER BY s.first_name, s.last_name`
    );

    const students = await Promise.all(result.rows.map(async (student) => {
      // Get latest check-in data
      const checkinResult = await pool.query(
        `SELECT emotion, emotion_intensity, stress_level 
         FROM daily_checkins 
         WHERE student_id = $1 
         ORDER BY date DESC 
         LIMIT 1`,
        [student.id]
      );

      return {
        id: student.id,
        name: `${student.first_name} ${student.last_name}`,
        email: student.email,
        grade: student.grade,
        lastCheckin: checkinResult.rows[0] || null,
        observationCount: parseInt(student.observation_count) || 0
      };
    }));

    res.json({ students });
  } catch (error) {
    console.error('Get students error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

module.exports = router;

