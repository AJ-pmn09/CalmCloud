/**
 * Original-style studentData.js for VM (clamcloud-backend).
 * Uses only mood_rating from daily_checkins (no "mood" column) so it works
 * when the DB has the older schema. Use this as routes/studentData.js on the VM.
 */
const express = require('express');
const { authenticateToken, requireRole } = require('../middleware/auth');
const { routeToSchoolDatabase } = require('../middleware/databaseRouter');

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
router.post('/student-data', authenticateToken, routeToSchoolDatabase, async (req, res) => {
  try {
    const { studentId, fhirData } = req.body;
    const userId = req.user.userId;
    const userRole = req.user.role;

    let targetStudentId = studentId;
    if (userRole === 'student') {
      const studentResult = await req.db.query(
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

    const studentCheck = await req.db.query(
      'SELECT id FROM students WHERE id = $1',
      [targetStudentId]
    );
    if (studentCheck.rows.length === 0) {
      return res.status(404).json({ error: 'Student not found' });
    }

    if (fhirData.observations && Array.isArray(fhirData.observations)) {
      for (const obs of fhirData.observations) {
        await req.db.query(
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

    const additionalNotes = req.body.additional_notes || req.body.additionalNotes || null;
    const additionalNotesJson = additionalNotes ? JSON.stringify({
      text: additionalNotes,
      studentId: targetStudentId,
      date: new Date().toISOString().split('T')[0],
      source: 'student_checkin',
      createdAt: new Date().toISOString()
    }) : null;
    if (fhirData.stressLevel != null || fhirData.moodRating != null || additionalNotes) {
      const stressObs = fhirData.observations?.find(o =>
        o.loinc_code === '73985-4' || o.code?.coding?.[0]?.code === '73985-4'
      );
      const stressVal = stressObs?.value_quantity ?? fhirData.stressLevel ?? null;
      const moodObs = fhirData.observations?.find(o =>
        o.loinc_code === '73830-5' || o.code?.coding?.[0]?.code === '73830-5'
      );
      const moodVal = moodObs?.value_quantity ?? fhirData.moodRating ?? null;

      const existing = await req.db.query(
        'SELECT id FROM daily_checkins WHERE student_id = $1 AND date = CURRENT_DATE',
        [targetStudentId]
      );
      if (existing.rows.length > 0) {
        await req.db.query(
          `UPDATE daily_checkins SET 
             stress_level = COALESCE($2, stress_level),
             mood_rating = COALESCE($3, mood_rating),
             additional_notes = COALESCE($4, additional_notes),
             additional_notes_json = COALESCE($5, additional_notes_json)
           WHERE student_id = $1 AND date = CURRENT_DATE`,
          [targetStudentId, stressVal, moodVal, additionalNotes, additionalNotesJson]
        );
      } else {
        await req.db.query(
          `INSERT INTO daily_checkins 
           (student_id, date, stress_level, mood_rating, additional_notes, additional_notes_json)
           VALUES ($1, CURRENT_DATE, $2, $3, $4, $5)`,
          [targetStudentId, stressVal, moodVal, additionalNotes, additionalNotesJson]
        );
      }
    }

    res.json({ success: true, message: 'Data saved' });
  } catch (error) {
    console.error('Save student data error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Get my student data (student role)
router.get('/student-data/me', authenticateToken, routeToSchoolDatabase, async (req, res) => {
  try {
    const userId = req.user.userId;
    const studentResult = await req.db.query(
      'SELECT * FROM students WHERE user_id = $1',
      [userId]
    );
    if (studentResult.rows.length === 0) {
      return res.status(404).json({ error: 'Student profile not found' });
    }
    const student = studentResult.rows[0];
    const studentId = student.id;

    const obsResult = await req.db.query(
      `SELECT * FROM fhir_observations 
       WHERE student_id = $1 
       ORDER BY effective_date_time DESC 
       LIMIT 100`,
      [studentId]
    );
    const observations = obsResult.rows.map(row => ({
      id: row.observation_id,
      resourceType: row.resource_type,
      status: row.status,
      code: {
        coding: [{ system: 'http://loinc.org', code: row.loinc_code, display: row.loinc_display }]
      },
      valueQuantity: row.value_quantity != null ? { value: row.value_quantity, unit: row.value_unit } : undefined,
      valueString: row.value_string,
      effectiveDateTime: row.effective_date_time,
      subject: { reference: row.subject_reference }
    }));

    // daily_checkins: only mood_rating (no "mood" column in original schema)
    let latestCheckin = null;
    try {
      const checkinResult = await req.db.query(
        `SELECT stress_level, mood_rating, date, stress_source, additional_notes
         FROM daily_checkins 
         WHERE student_id = $1 
         ORDER BY date DESC, created_at DESC 
         LIMIT 1`,
        [studentId]
      );
      latestCheckin = checkinResult.rows[0] || null;
    } catch (checkinError) {
      console.error('Error fetching check-in data:', checkinError.message);
      latestCheckin = null;
    }

    let activityData = null;
    try {
      const activityResult = await req.db.query(
        `SELECT heart_rate, steps, sleep_hours, hydration_percent, nutrition_percent, mood as activity_mood, stress_level as activity_stress
         FROM activity_logs 
         WHERE student_id = $1 
         ORDER BY date DESC, created_at DESC 
         LIMIT 1`,
        [studentId]
      );
      activityData = activityResult.rows[0] || null;
    } catch (activityError) {
      console.error('Error fetching activity data:', activityError.message);
      activityData = null;
    }

    const response = {
      success: true,
      student: {
        id: student.id,
        firstName: student.first_name,
        lastName: student.last_name,
        email: student.email,
        grade: student.grade,
        schoolId: student.school_id,
        profilePictureUrl: student.profile_picture_url || null
      },
      fhirData: { observations: observations || [] },
      latestCheckin: latestCheckin ? {
        stress_level: latestCheckin.stress_level,
        mood_rating: latestCheckin.mood_rating,
        mood: latestCheckin.mood_rating,
        date: latestCheckin.date ? new Date(latestCheckin.date).toISOString().split('T')[0] : null,
        stress_source: latestCheckin.stress_source,
        additional_notes: latestCheckin.additional_notes
      } : null,
      activityData: activityData ? {
        heart_rate: activityData.heart_rate,
        steps: activityData.steps,
        sleep_hours: activityData.sleep_hours ? parseFloat(activityData.sleep_hours) : null,
        hydration_percent: activityData.hydration_percent,
        nutrition_percent: activityData.nutrition_percent,
        mood: activityData.activity_mood || null,
        stress_level: activityData.activity_stress || null
      } : null
    };

    console.log(`[${new Date().toISOString()}] GET /api/student-data/me - Success: studentId=${studentId}, hasCheckin=${!!latestCheckin}, hasActivity=${!!activityData}`);
    res.json(response);
  } catch (error) {
    console.error('Get student data error:', error);
    res.status(500).json({
      error: 'Server error',
      ...(process.env.NODE_ENV !== 'production' && { details: error.message })
    });
  }
});

// Get historical trend data
router.get('/student-data/trends', authenticateToken, requireRole('student'), routeToSchoolDatabase, async (req, res) => {
  try {
    const userId = req.user.userId;
    const days = parseInt(req.query.days) || 7;
    if (days < 1 || days > 30) {
      return res.status(400).json({ error: 'Days must be between 1 and 30' });
    }

    const studentResult = await req.db.query(
      'SELECT id FROM students WHERE user_id = $1',
      [userId]
    );
    if (studentResult.rows.length === 0) {
      return res.status(404).json({ error: 'Student profile not found' });
    }
    const studentId = studentResult.rows[0].id;

    let checkins = [];
    try {
      const checkinResult = await req.db.query(
        `SELECT date, stress_level, mood_rating, stress_source, additional_notes
         FROM daily_checkins 
         WHERE student_id = $1 
           AND date >= CURRENT_DATE - INTERVAL '${days} days'
         ORDER BY date ASC`,
        [studentId]
      );
      checkins = checkinResult.rows;
    } catch (checkinError) {
      console.error('Error fetching check-in trends:', checkinError.message);
      checkins = [];
    }

    let activities = [];
    try {
      const activityResult = await req.db.query(
        `SELECT date, heart_rate, steps, sleep_hours, hydration_percent, nutrition_percent, mood as activity_mood, stress_level as activity_stress
         FROM activity_logs 
         WHERE student_id = $1 
           AND date >= CURRENT_DATE - INTERVAL '${days} days'
         ORDER BY date ASC`,
        [studentId]
      );
      activities = activityResult.rows;
    } catch (activityError) {
      console.error('Error fetching activity trends:', activityError.message);
      activities = [];
    }

    res.json({
      success: true,
      days: days,
      checkins: checkins.map(c => ({
        date: c.date ? new Date(c.date).toISOString().split('T')[0] : null,
        stressLevel: c.stress_level,
        moodRating: c.mood_rating,
        mood: c.mood_rating,
        stressSource: c.stress_source,
        additionalNotes: c.additional_notes
      })),
      activities: activities.map(a => ({
        date: a.date ? new Date(a.date).toISOString().split('T')[0] : null,
        heartRate: a.heart_rate,
        steps: a.steps,
        sleepHours: a.sleep_hours ? parseFloat(a.sleep_hours) : null,
        hydrationPercent: a.hydration_percent,
        nutritionPercent: a.nutrition_percent,
        mood: a.activity_mood,
        stressLevel: a.activity_stress
      }))
    });
  } catch (error) {
    console.error('Get trends error:', error);
    res.status(500).json({
      error: 'Server error',
      ...(process.env.NODE_ENV !== 'production' && { details: error.message })
    });
  }
});

// Get student FHIR data by ID
router.get('/student-data/:studentId', authenticateToken, routeToSchoolDatabase, async (req, res) => {
  try {
    const { studentId } = req.params;
    const userId = req.user.userId;
    const userRole = req.user.role;

    if (userRole === 'student') {
      const studentResult = await req.db.query(
        'SELECT id FROM students WHERE user_id = $1 AND id = $2',
        [userId, studentId]
      );
      if (studentResult.rows.length === 0) {
        return res.status(403).json({ error: 'Access denied' });
      }
    } else if (userRole === 'parent') {
      const linkResult = await req.db.query(
        'SELECT id FROM parent_child_links WHERE parent_id = $1 AND student_id = $2 AND status = $3',
        [userId, studentId, 'active']
      );
      if (linkResult.rows.length === 0) {
        return res.status(403).json({ error: 'Access denied - not linked to this student' });
      }
    }

    const studentResult = await req.db.query(
      `SELECT s.*, u.email, 
       COALESCE(TRIM(u.first_name || ' ' || u.last_name), u.first_name, u.last_name, 'User') as user_name 
       FROM students s 
       JOIN users u ON s.user_id = u.id 
       WHERE s.id = $1`,
      [studentId]
    );
    if (studentResult.rows.length === 0) {
      return res.status(404).json({ error: 'Student not found' });
    }
    const student = studentResult.rows[0];

    const observationsResult = await req.db.query(
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
        coding: [{ system: 'http://loinc.org', code: row.loinc_code, display: row.loinc_display }]
      },
      valueQuantity: row.value_quantity ? { value: row.value_quantity, unit: row.value_unit } : undefined,
      valueString: row.value_string,
      effectiveDateTime: row.effective_date_time,
      subject: { reference: row.subject_reference }
    }));

    res.json({
      student: {
        id: student.id,
        name: `${student.first_name} ${student.last_name}`,
        email: student.email
      },
      fhirData: { observations },
      isParentView: userRole === 'parent'
    });
  } catch (error) {
    console.error('Get student data error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Get all students
router.get('/students', authenticateToken, requireRole('associate', 'expert', 'staff', 'admin'), routeToSchoolDatabase, async (req, res) => {
  try {
    const result = await req.db.query(
      `SELECT s.*, u.email, 
       COALESCE(TRIM(u.first_name || ' ' || u.last_name), u.first_name, u.last_name, 'User') as user_name,
       (SELECT COUNT(*) FROM fhir_observations WHERE student_id = s.id) as observation_count,
       (SELECT MAX(effective_date_time) FROM fhir_observations WHERE student_id = s.id) as last_checkin
       FROM students s
       JOIN users u ON s.user_id = u.id
       ORDER BY s.first_name, s.last_name`
    );

    const students = await Promise.all(result.rows.map(async (student) => {
      const checkinResult = await req.db.query(
        `SELECT stress_level, mood_rating, date
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
