const express = require('express');
const { authenticateToken, requireRole } = require('../middleware/auth');
const { routeToSchoolDatabase } = require('../middleware/databaseRouter');
const { getPool } = require('../config/databaseManager');

const router = express.Router();

// Get activity logs for achievements calculation (matches dashboard logic)
router.get('/activity-logs', authenticateToken, requireRole('student'), routeToSchoolDatabase, async (req, res) => {
  try {
    const { days = 30 } = req.query; // Default to 30 days
    const userId = req.user.userId;
    const schoolName = req.user.schoolName;
    const schoolPool = req.db || (schoolName ? getPool(schoolName) : null);
    
    if (!schoolPool) {
      return res.status(500).json({ error: 'Database connection error' });
    }

    // Get student ID
    const studentResult = await schoolPool.query(
      'SELECT id FROM students WHERE user_id = $1',
      [userId]
    );

    if (studentResult.rows.length === 0) {
      return res.status(404).json({ error: 'Student profile not found' });
    }

    const studentId = studentResult.rows[0].id;
    const daysInt = parseInt(days) || 30;

    // First, get count of all logs (for debugging)
    const countResult = await schoolPool.query(
      `SELECT COUNT(*) as total_count,
              MIN(date) as earliest_date,
              MAX(date) as latest_date
       FROM activity_logs
       WHERE student_id = $1`,
      [studentId]
    );

    // Get activity logs with all required fields for achievements calculation
    // Using parameterized query to prevent SQL injection
    const result = await schoolPool.query(
      `SELECT 
         date,
         COALESCE(steps, 0) as steps,
         COALESCE(sleep_hours::numeric, 0) as sleep_hours,
         COALESCE(hydration_percent, 0) as hydration_percent,
         COALESCE(nutrition_percent, 0) as nutrition_percent
       FROM activity_logs
       WHERE student_id = $1
         AND date >= CURRENT_DATE - INTERVAL '${daysInt} days'
       ORDER BY date DESC`,
      [studentId]
    );

    // Format logs to match dashboard structure
    // Ensure date is in YYYY-MM-DD format
    const logs = result.rows.map(row => {
      let dateStr = row.date;
      // If date is a Date object, format it
      if (dateStr instanceof Date) {
        dateStr = dateStr.toISOString().split('T')[0];
      } else if (typeof dateStr === 'string' && dateStr.includes('T')) {
        // If it's an ISO string, extract just the date part
        dateStr = dateStr.split('T')[0];
      }
      
      return {
        date: dateStr, // YYYY-MM-DD format
        steps: parseInt(row.steps) || 0,
        sleep_hours: parseFloat(row.sleep_hours) || 0,
        hydration_percent: parseInt(row.hydration_percent) || 0,
        nutrition_percent: parseInt(row.nutrition_percent) || 0
      };
    });

    // Log for debugging
    console.log(`[Activity Logs] Student ${studentId}, Requested ${daysInt} days, Found ${logs.length} logs`);
    console.log(`[Activity Logs] Total logs in DB: ${countResult.rows[0]?.total_count || 0}, Date range: ${countResult.rows[0]?.earliest_date || 'N/A'} to ${countResult.rows[0]?.latest_date || 'N/A'}`);

    res.json({ 
      logs,
      meta: {
        requestedDays: daysInt,
        returnedCount: logs.length,
        totalInDb: parseInt(countResult.rows[0]?.total_count || 0),
        dateRange: {
          earliest: countResult.rows[0]?.earliest_date || null,
          latest: countResult.rows[0]?.latest_date || null
        }
      }
    });
  } catch (error) {
    console.error('Get activity logs error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Sync wearable/Health Connect data for today (upsert by student_id + date)
// Body: { steps?, sleepMinutes?, heartRate?, date? } — date is YYYY-MM-DD, default today
router.post('/activity-logs/sync', authenticateToken, requireRole('student'), routeToSchoolDatabase, async (req, res) => {
  try {
    const userId = req.user.userId;
    const schoolPool = req.db;

    if (!schoolPool) {
      return res.status(500).json({ error: 'Database connection error' });
    }

    const studentResult = await schoolPool.query(
      'SELECT id FROM students WHERE user_id = $1',
      [userId]
    );
    if (studentResult.rows.length === 0) {
      return res.status(404).json({ error: 'Student profile not found' });
    }
    const studentId = studentResult.rows[0].id;

    const { steps, sleepMinutes, heartRate, date: dateStr } = req.body || {};
    const date = dateStr && /^\d{4}-\d{2}-\d{2}$/.test(dateStr) ? dateStr : new Date().toISOString().split('T')[0];

    const stepsVal = steps != null ? parseInt(steps, 10) : null;
    const sleepHoursVal = sleepMinutes != null ? Math.round((sleepMinutes / 60) * 100) / 100 : null;
    const heartRateVal = heartRate != null ? Math.round(Number(heartRate)) : null;

    if (stepsVal == null && sleepHoursVal == null && heartRateVal == null) {
      return res.status(400).json({ error: 'Provide at least one of steps, sleepMinutes, heartRate' });
    }

    const existing = await schoolPool.query(
      'SELECT id FROM activity_logs WHERE student_id = $1 AND date = $2::date',
      [studentId, date]
    );
    if (existing.rows.length > 0) {
      await schoolPool.query(
        `UPDATE activity_logs SET
           steps = CASE WHEN $3 IS NOT NULL THEN $3 ELSE steps END,
           sleep_hours = CASE WHEN $4 IS NOT NULL THEN $4 ELSE sleep_hours END,
           heart_rate = CASE WHEN $5 IS NOT NULL THEN $5 ELSE heart_rate END
         WHERE student_id = $1 AND date = $2::date`,
        [studentId, date, stepsVal, sleepHoursVal, heartRateVal]
      );
    } else {
      await schoolPool.query(
        `INSERT INTO activity_logs (student_id, date, steps, sleep_hours, heart_rate)
         VALUES ($1, $2::date, $3, $4, $5)`,
        [studentId, date, stepsVal ?? 0, sleepHoursVal ?? 0, heartRateVal]
      );
    }

    res.json({ success: true, date });
  } catch (error) {
    console.error('Activity sync error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

module.exports = router;
