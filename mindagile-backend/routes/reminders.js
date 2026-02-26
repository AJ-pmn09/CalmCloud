const express = require('express');
const pool = require('../config/database');
const { authenticateToken, requireRole } = require('../middleware/auth');

const router = express.Router();

// Helper function to log audit events
async function logAuditEvent(userId, userRole, actionType, resourceType, resourceId, details = {}) {
  try {
    await pool.query(
      `INSERT INTO audit_logs (user_id, user_role, action_type, resource_type, resource_id, details)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [userId, userRole, actionType, resourceType, resourceId, JSON.stringify(details)]
    );
  } catch (error) {
    console.error('Audit log error:', error);
  }
}

    // Get reminder configuration for student (from students table - matches Mindaigle structure)
    router.get('/student-reminder-config', authenticateToken, requireRole('student'), async (req, res) => {
      try {
        const userId = req.user.userId;

        // Get student with reminder config (embedded in students table)
        const result = await pool.query(
          `SELECT s.id, s.reminder_enabled, s.reminder_interval_hours, 
                  s.last_missed_checkin_at, s.last_reminder_sent_at,
                  s.reminder_config_updated_at,
                  s.reminder_smart_scheduling, s.reminder_quiet_hours_start, 
                  s.reminder_quiet_hours_end, s.reminder_max_per_day
           FROM students s
           WHERE s.user_id = $1`,
          [userId]
        );

        if (result.rows.length === 0) {
          return res.status(404).json({ error: 'Student profile not found' });
        }

        const student = result.rows[0];

        // Get last check-in date
        const checkinResult = await pool.query(
          `SELECT MAX(date) as last_checkin_date 
           FROM daily_checkins 
           WHERE student_id = $1`,
          [student.id]
        );

        res.json({
          studentId: student.id,
          reminderEnabled: student.reminder_enabled ?? true,
          reminderIntervalHours: student.reminder_interval_hours ?? 24,
          lastMissedCheckinAt: student.last_missed_checkin_at,
          lastReminderSentAt: student.last_reminder_sent_at,
          lastCheckinDate: checkinResult.rows[0]?.last_checkin_date || null,
          smartScheduling: student.reminder_smart_scheduling ?? true,
          quietHoursStart: student.reminder_quiet_hours_start || '22:00:00',
          quietHoursEnd: student.reminder_quiet_hours_end || '07:00:00',
          maxPerDay: student.reminder_max_per_day || 3
        });
      } catch (error) {
        console.error('Get reminder config error:', error);
        res.status(500).json({ error: 'Server error', details: error.message });
      }
    });

    // Update reminder configuration (updates students table - matches Mindaigle structure)
    router.put('/student-reminder-config', authenticateToken, requireRole('student'), async (req, res) => {
      try {
        const { reminderEnabled, reminderIntervalHours, smartScheduling, quietHoursStart, quietHoursEnd, maxPerDay } = req.body;
        const userId = req.user.userId;

        // Get student ID
        const studentResult = await pool.query(
          'SELECT id FROM students WHERE user_id = $1',
          [userId]
        );

        if (studentResult.rows.length === 0) {
          return res.status(404).json({ error: 'Student profile not found' });
        }

        const studentId = studentResult.rows[0].id;

        // Validate interval
        if (reminderIntervalHours !== undefined && (reminderIntervalHours < 1 || reminderIntervalHours > 168)) {
          return res.status(400).json({ error: 'Reminder interval must be between 1 and 168 hours (1 week)' });
        }

        // Validate max per day
        if (maxPerDay !== undefined && (maxPerDay < 1 || maxPerDay > 10)) {
          return res.status(400).json({ error: 'Max reminders per day must be between 1 and 10' });
        }

        // Update config in students table
        const updateFields = [];
        const updateValues = [];
        let paramCount = 1;

        if (reminderEnabled !== undefined) {
          updateFields.push(`reminder_enabled = $${paramCount++}`);
          updateValues.push(reminderEnabled);
        }

        if (reminderIntervalHours !== undefined) {
          updateFields.push(`reminder_interval_hours = $${paramCount++}`);
          updateValues.push(reminderIntervalHours);
        }

        if (smartScheduling !== undefined) {
          updateFields.push(`reminder_smart_scheduling = $${paramCount++}`);
          updateValues.push(smartScheduling);
        }

        if (quietHoursStart !== undefined) {
          updateFields.push(`reminder_quiet_hours_start = $${paramCount++}`);
          updateValues.push(quietHoursStart);
        }

        if (quietHoursEnd !== undefined) {
          updateFields.push(`reminder_quiet_hours_end = $${paramCount++}`);
          updateValues.push(quietHoursEnd);
        }

        if (maxPerDay !== undefined) {
          updateFields.push(`reminder_max_per_day = $${paramCount++}`);
          updateValues.push(maxPerDay);
        }

        updateFields.push(`reminder_config_updated_at = $${paramCount++}`);
        updateValues.push(new Date().toISOString());
        updateValues.push(studentId);

        const result = await pool.query(
          `UPDATE students 
           SET ${updateFields.join(', ')}
           WHERE id = $${paramCount}
           RETURNING id, reminder_enabled, reminder_interval_hours, 
                     last_missed_checkin_at, last_reminder_sent_at, reminder_config_updated_at,
                     reminder_smart_scheduling, reminder_quiet_hours_start, 
                     reminder_quiet_hours_end, reminder_max_per_day`,
          updateValues
        );

        if (result.rows.length === 0) {
          return res.status(404).json({ error: 'Student not found' });
        }

        await logAuditEvent(userId, req.user.role, 'reminder_config_updated', 'reminder_config', studentId, {
          reminderEnabled, reminderIntervalHours, smartScheduling, quietHoursStart, quietHoursEnd, maxPerDay
        });

        res.json({ success: true, config: result.rows[0] });
      } catch (error) {
        console.error('Update reminder config error:', error);
        res.status(500).json({ error: 'Server error', details: error.message });
      }
    });

    // Get students with missed check-ins (for background jobs - uses embedded fields)
    router.get('/students-missed-checkins', authenticateToken, requireRole('associate', 'expert', 'admin'), async (req, res) => {
      try {
        const result = await pool.query(
          `SELECT s.id, s.first_name, s.last_name, s.user_id,
              s.reminder_enabled, s.reminder_interval_hours,
              s.last_missed_checkin_at, s.last_reminder_sent_at,
              (SELECT MAX(date) FROM daily_checkins WHERE student_id = s.id) as last_checkin_date
       FROM students s
       WHERE s.reminder_enabled = true
         AND (
           (SELECT MAX(date) FROM daily_checkins WHERE student_id = s.id) IS NULL
           OR (SELECT MAX(date) FROM daily_checkins WHERE student_id = s.id) < CURRENT_DATE - INTERVAL '1 day'
         )
         AND (
           s.last_reminder_sent_at IS NULL
           OR s.last_reminder_sent_at < NOW() - (s.reminder_interval_hours || ' hours')::INTERVAL
         )
       ORDER BY s.first_name, s.last_name`
        );

        res.json({ students: result.rows });
      } catch (error) {
        console.error('Get missed check-ins error:', error);
        res.status(500).json({ error: 'Server error', details: error.message });
      }
    });

    // Get reminder logs (for staff dashboard)
    router.get('/reminder-logs', authenticateToken, requireRole('associate', 'expert', 'admin'), async (req, res) => {
      try {
        const { studentId, escalationLevel, limit = 50, offset = 0 } = req.query;
        
        let query = `
          SELECT rl.*, 
                 s.first_name || ' ' || s.last_name as student_name,
                 s.grade
          FROM reminder_logs rl
          JOIN students s ON rl.student_id = s.id
          WHERE 1=1
        `;
        const params = [];
        let paramCount = 1;
        
        if (studentId) {
          query += ` AND rl.student_id = $${paramCount++}`;
          params.push(parseInt(studentId));
        }
        
        if (escalationLevel) {
          query += ` AND rl.escalation_level = $${paramCount++}`;
          params.push(escalationLevel);
        }
        
        query += ` ORDER BY rl.sent_at DESC LIMIT $${paramCount++} OFFSET $${paramCount++}`;
        params.push(parseInt(limit), parseInt(offset));
        
        const result = await pool.query(query, params);
        
        const logs = result.rows.map(row => ({
          id: row.id,
          studentId: row.student_id,
          studentName: row.student_name,
          grade: row.grade,
          reminderType: row.reminder_type,
          escalationLevel: row.escalation_level,
          reminderIntervalHours: row.reminder_interval_hours,
          daysSinceCheckin: row.days_since_checkin,
          sentAt: row.sent_at,
          notificationMethod: row.notification_method,
          status: row.status
        }));
        
        res.json({ logs });
      } catch (error) {
        console.error('Get reminder logs error:', error);
        res.status(500).json({ error: 'Server error', details: error.message });
      }
    });

module.exports = router;

