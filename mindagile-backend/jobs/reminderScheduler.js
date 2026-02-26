/**
 * Background Job Scheduler for Missed Check-in Reminders
 * 
 * This script should be run as a cron job or scheduled task to:
 * 1. Check for students who have missed check-ins
 * 2. Send reminders based on their reminder configuration
 * 3. Update reminder timestamps
 * 
 * Run this script periodically (e.g., every hour):
 * node jobs/reminderScheduler.js
 * 
 * Or set up as a cron job:
 * 0 * * * * cd /path/to/mindaigle-backend && node jobs/reminderScheduler.js
 */

const { getAllPools } = require('../config/databaseManager');

async function checkAndSendReminders() {
  try {
    console.log(`[${new Date().toISOString()}] Starting reminder check...`);

    // Get all school database pools
    const allPools = getAllPools();
    let totalStudents = 0;

    // Check each school database for students with missed check-ins
    for (const [schoolName, pool] of Object.entries(allPools)) {
      try {
        // Check if reminder_enabled column exists (avoid query error and repeated SKIP logs)
        const colCheck = await pool.query(
          `SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'students' AND column_name = 'reminder_enabled'`
        );
        if (colCheck.rows.length === 0) {
          continue; // Column not in schema – skip this school silently
        }

        const result = await pool.query(
          `SELECT s.id, s.first_name, s.last_name, s.user_id,
                  u.email,
                  s.reminder_enabled, s.reminder_interval_hours,
                  s.last_missed_checkin_at, s.last_reminder_sent_at,
                  (SELECT MAX(date) FROM daily_checkins WHERE student_id = s.id) as last_checkin_date,
                  CASE 
                    WHEN (SELECT MAX(date) FROM daily_checkins WHERE student_id = s.id) IS NULL 
                      THEN EXTRACT(EPOCH FROM (NOW() - s.created_at::timestamp)) / 3600
                    ELSE EXTRACT(EPOCH FROM (CURRENT_DATE::timestamp - (SELECT MAX(date) FROM daily_checkins WHERE student_id = s.id)::timestamp)) / 86400 * 24
                  END as hours_since_checkin
           FROM students s
           JOIN users u ON s.user_id = u.id
           WHERE s.reminder_enabled = true
             AND (
               (SELECT MAX(date) FROM daily_checkins WHERE student_id = s.id) IS NULL
               OR (SELECT MAX(date) FROM daily_checkins WHERE student_id = s.id) < CURRENT_DATE - INTERVAL '1 day'
             )
           ORDER BY s.first_name, s.last_name`
        );

        const students = result.rows;
        console.log(`[${schoolName}] Found ${students.length} students with missed check-ins`);
        totalStudents += students.length;

    for (const student of students) {
      try {
        const hoursSinceCheckin = parseFloat(student.hours_since_checkin) || 0;
        const daysSinceCheckin = Math.floor(hoursSinceCheckin / 24);
        
        // Determine escalation level and reminder interval
        let escalationLevel = 'normal';
        let reminderInterval = student.reminder_interval_hours || 24;
        let shouldSendReminder = false;
        
        if (daysSinceCheckin >= 3) {
          // 72+ hours: High priority escalation - notify staff
          escalationLevel = 'critical';
          reminderInterval = 24; // Check every 24 hours
          shouldSendReminder = true;
          
          // Create alert for mental health staff (if table exists)
          try {
            await pool.query(
              `INSERT INTO emergency_alerts (student_id, alert_type, status, message)
               VALUES ($1, 'urgent', 'active', $2)
               ON CONFLICT DO NOTHING`,
              [
                student.id,
                `Student ${student.first_name} ${student.last_name} has missed check-ins for ${daysSinceCheckin} days. Please follow up.`
              ]
            );
          } catch (alertError) {
            // Table doesn't exist - log but don't fail
            if (alertError.code === '42P01') {
              console.log(`[${schoolName}] emergency_alerts table not found - skipping alert creation for student ${student.id}`);
            } else {
              console.error(`[${schoolName}] Error creating emergency alert for student ${student.id}:`, alertError.message);
            }
          }
        } else if (daysSinceCheckin >= 2) {
          // 48-72 hours: Medium escalation
          escalationLevel = 'escalated';
          reminderInterval = 24;
          shouldSendReminder = !student.last_reminder_sent_at || 
            (new Date() - new Date(student.last_reminder_sent_at)) >= 24 * 60 * 60 * 1000;
        } else if (daysSinceCheckin >= 1) {
          // 24-48 hours: First reminder
          escalationLevel = 'normal';
          reminderInterval = student.reminder_interval_hours || 24;
          shouldSendReminder = !student.last_reminder_sent_at || 
            (new Date() - new Date(student.last_reminder_sent_at)) >= reminderInterval * 60 * 60 * 1000;
        }

        if (!shouldSendReminder) {
          continue; // Skip if not time to send reminder yet
        }

        // Update last_missed_checkin_at if not set (in students table per Mindaigle)
        if (!student.last_missed_checkin_at) {
          await pool.query(
            `UPDATE students 
             SET last_missed_checkin_at = CURRENT_TIMESTAMP
             WHERE id = $1`,
            [student.id]
          );
        }

        // Simple check: don't send if we already sent one in the last reminder_interval_hours
        // This prevents notification fatigue without requiring additional columns
        if (student.last_reminder_sent_at) {
          const lastSent = new Date(student.last_reminder_sent_at);
          const hoursSinceLastReminder = (new Date() - lastSent) / (1000 * 60 * 60);
          
          if (hoursSinceLastReminder < reminderInterval) {
            console.log(`[SKIP] Reminder sent recently (${Math.round(hoursSinceLastReminder)}h ago) for ${student.first_name} ${student.last_name}`);
            continue;
          }
        }

        // Update last_reminder_sent_at (in students table per Mindaigle)
        await pool.query(
          `UPDATE students 
           SET last_reminder_sent_at = CURRENT_TIMESTAMP
           WHERE id = $1`,
          [student.id]
        );

        // Log reminder to reminder_logs table (if exists)
        let reminderLogId = null;
        try {
          const reminderLogResult = await pool.query(
            `INSERT INTO reminder_logs 
             (student_id, reminder_type, escalation_level, reminder_interval_hours, days_since_checkin, status)
             VALUES ($1, 'missed_checkin', $2, $3, $4, 'sent')
             RETURNING id`,
            [student.id, escalationLevel, reminderInterval, daysSinceCheckin]
          );
          reminderLogId = reminderLogResult.rows[0]?.id;
        } catch (logError) {
          // Table doesn't exist - that's OK, continue without logging
          if (logError.code !== '42P01') {
            console.error(`[${schoolName}] Error logging reminder for student ${student.id}:`, logError.message);
          }
        }

        // Log audit event with escalation level (if audit_logs table exists)
        try {
          await pool.query(
            `INSERT INTO audit_logs (user_id, user_role, action_type, resource_type, resource_id, details)
             VALUES ($1, 'system', 'reminder_sent', 'reminder', $2, $3)`,
            [
              student.user_id,
              student.id,
              JSON.stringify({
                reminderLogId: reminderLogId,
                reminderIntervalHours: reminderInterval,
                lastCheckinDate: student.last_checkin_date,
                daysSinceCheckin: daysSinceCheckin,
                escalationLevel: escalationLevel
              })
            ]
          );
        } catch (auditError) {
          // Table doesn't exist - that's OK, continue without audit logging
          if (auditError.code !== '42P01') {
            console.error(`[${schoolName}] Error logging audit for student ${student.id}:`, auditError.message);
          }
        }

        // Send in-app message to student so they see it in Messages (Wellness / Chat)
        let reminderSenderId = null;
        try {
          const senderResult = await pool.query(
            `SELECT id FROM users WHERE role IN ('associate', 'expert', 'staff', 'admin') LIMIT 1`
          );
          if (senderResult.rows.length > 0) {
            reminderSenderId = senderResult.rows[0].id;
          }
        } catch (e) {
          // ignore
        }
        if (reminderSenderId != null) {
          const reminderSubject = daysSinceCheckin >= 2
            ? 'Check-in reminder'
            : 'Quick check-in';
          const reminderBody = daysSinceCheckin >= 3
            ? `Hi ${student.first_name}, we noticed you haven’t checked in for ${daysSinceCheckin} days. Your wellness matters — when you can, open MindAIgle and do a quick check-in. If you’d like to talk, reach out to your counselor.`
            : `Hi ${student.first_name}, when you have a moment, open MindAIgle and do a quick check-in so we can see how you’re doing.`;
          try {
            await pool.query(
              `INSERT INTO communications (sender_id, sender_role, recipient_type, recipient_id, subject, message, priority, parent_visible, emergency_override, status)
               VALUES ($1, 'associate', 'student', $2, $3, $4, 'normal', true, false, 'sent')`,
              [reminderSenderId, student.id, reminderSubject, reminderBody]
            );
            console.log(`[REMINDER] In-app message created for ${student.first_name} ${student.last_name}`);
          } catch (commErr) {
            if (commErr.code !== '42P01') {
              console.error(`[${schoolName}] Error creating reminder communication for student ${student.id}:`, commErr.message);
            }
          }
        }

        console.log(
          `[REMINDER ${escalationLevel.toUpperCase()}] Sent to ${student.first_name} ${student.last_name} (${student.email}) - ` +
          `Last check-in: ${student.last_checkin_date || 'Never'} (${daysSinceCheckin} days ago)`
        );
      } catch (error) {
        console.error(`Error processing reminder for student ${student.id}:`, error);
      }
    }
      } catch (error) {
        console.error(`Error checking ${schoolName} database:`, error.message);
        // Continue with other databases even if one fails
      }
    }

    console.log(`[${new Date().toISOString()}] Reminder check completed. Total students checked: ${totalStudents}`);
  } catch (error) {
    console.error('Error in reminder scheduler:', error);
    process.exit(1);
  }
}

// Run if called directly
if (require.main === module) {
  checkAndSendReminders()
    .then(() => {
      console.log('Reminder scheduler finished successfully');
      process.exit(0);
    })
    .catch((error) => {
      console.error('Reminder scheduler failed:', error);
      process.exit(1);
    });
}

module.exports = { checkAndSendReminders };

