const express = require('express');
const pool = require('../config/database');
const { authenticateToken, requireRole } = require('../middleware/auth');
const { routeToSchoolDatabase } = require('../middleware/databaseRouter');

const router = express.Router();

// Helper function to log audit events (uses school-specific database)
async function logAuditEvent(userId, userRole, actionType, resourceType, resourceId, details = {}, schoolPool = null) {
  try {
    const dbPool = schoolPool || pool; // Use provided pool or fallback to default
    await dbPool.query(
      `INSERT INTO audit_logs (user_id, user_role, action_type, resource_type, resource_id, details)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [userId, userRole, actionType, resourceType, resourceId, JSON.stringify(details)]
    );
  } catch (error) {
    console.error('Audit log error:', error);
  }
}

    // Create emergency alert (students only - matches Mindaigle structure, no location field)
    // Enhanced with immediate notification to on-call staff and suicide-risk screening
    router.post('/emergency-alert', authenticateToken, requireRole('student'), routeToSchoolDatabase, async (req, res) => {
      try {
        const { alertType, message, suicideRiskScreening } = req.body;
        const userId = req.user.userId;
        const schoolName = req.user.schoolName;

        // Get the correct database pool for this school (from middleware or fallback)
        const schoolPool = req.db || (schoolName ? getPool(schoolName) : pool);
        
        // Debug logging
        console.log(`[ALERT CREATE] User ${userId}, schoolName: ${schoolName}, has req.db: ${!!req.db}`);
        if (schoolPool) {
          const poolConfig = schoolPool.options || {};
          console.log(`[ALERT CREATE] Using database: ${poolConfig.database || 'unknown'} on port ${poolConfig.port || 'unknown'}`);
        }
        
        if (!schoolName) {
          return res.status(400).json({ error: 'School not identified. Please log in again.' });
        }
        
        if (!schoolPool) {
          console.error(`[ALERT CREATE] No database pool available for school: ${schoolName}`);
          return res.status(500).json({ error: 'Database connection error', details: `Could not connect to database for school: ${schoolName}` });
        }

        // Get student ID and info
        const studentResult = await schoolPool.query(
          `SELECT s.id, s.first_name, s.last_name, u.email 
           FROM students s 
           JOIN users u ON s.user_id = u.id 
           WHERE s.user_id = $1`,
          [userId]
        );

        if (studentResult.rows.length === 0) {
          return res.status(404).json({ error: 'Student profile not found' });
        }

        const student = studentResult.rows[0];
        const studentId = student.id;

        // Validate alert type
        const validTypes = ['emergency', 'urgent', 'support'];
        const finalAlertType = validTypes.includes(alertType) ? alertType : 'emergency';

        // Create alert (no location field per Mindaigle structure)
        const result = await schoolPool.query(
          `INSERT INTO emergency_alerts 
           (student_id, alert_type, status, message)
           VALUES ($1, $2, 'active', $3)
           RETURNING *`,
          [studentId, finalAlertType, message || 'Emergency assistance needed']
        );

        const alert = result.rows[0];

        // Store suicide-risk screening if provided
        if (suicideRiskScreening && typeof suicideRiskScreening === 'object') {
          try {
            // Calculate risk score (0-10) based on responses
            let riskScore = 0;
            const questions = suicideRiskScreening.questions || [];
            
            // Simple scoring: each "yes" or high-risk answer adds to score
            questions.forEach((q, index) => {
              if (q.response === 'yes' || q.response === true || q.response === 'high') {
                riskScore += 2; // Each high-risk answer = 2 points
              } else if (q.response === 'sometimes' || q.response === 'moderate') {
                riskScore += 1; // Moderate risk = 1 point
              }
            });
            
            // Cap at 10
            riskScore = Math.min(riskScore, 10);
            
            // Determine risk level
            let riskLevel = 'low';
            let immediateAction = false;
            if (riskScore >= 8) {
              riskLevel = 'critical';
              immediateAction = true;
            } else if (riskScore >= 5) {
              riskLevel = 'high';
              immediateAction = true;
            } else if (riskScore >= 3) {
              riskLevel = 'moderate';
            }
            
            // Store screening in database
            await schoolPool.query(
              `INSERT INTO suicide_risk_screenings 
               (student_id, emergency_alert_id, screening_questions, risk_score, risk_level, immediate_action_required)
               VALUES ($1, $2, $3, $4, $5, $6)`,
              [
                studentId,
                alert.id,
                JSON.stringify(suicideRiskScreening),
                riskScore,
                riskLevel,
                immediateAction
              ]
            );
            
            // If critical risk, create additional urgent alert
            if (immediateAction && riskLevel === 'critical') {
              await schoolPool.query(
                `INSERT INTO emergency_alerts (student_id, alert_type, status, message)
                 VALUES ($1, 'emergency', 'active', $2)`,
                [
                  studentId,
                  `CRITICAL SUICIDE RISK: ${student.first_name} ${student.last_name} - Immediate intervention required. Risk score: ${riskScore}/10`
                ]
              );
            }
          } catch (screeningError) {
            console.error('Error storing suicide-risk screening:', screeningError);
            // Don't fail the alert creation if screening storage fails
          }
        }

        // Immediate notification to on-call staff
        // Get all associates, experts, and staff who should be notified (from same school)
        const staffResult = await schoolPool.query(
          `SELECT u.id, u.email, u.first_name, u.last_name, u.role
           FROM users u
           WHERE u.role IN ('associate', 'expert', 'staff', 'admin')
           ORDER BY 
             CASE u.role
               WHEN 'admin' THEN 1
               WHEN 'expert' THEN 2
               WHEN 'associate' THEN 3
               WHEN 'staff' THEN 4
               ELSE 5
             END
           LIMIT 10`
        );

        // Create high-priority communication to notify staff
        // This ensures immediate visibility in staff dashboards
        for (const staff of staffResult.rows) {
          try {
            await schoolPool.query(
              `INSERT INTO communications 
               (sender_id, sender_role, recipient_type, recipient_id, recipient_cohort,
                subject, message, priority, parent_visible, emergency_override, status)
               VALUES ($1, 'system', 'student', $2, NULL,
                $3, $4, 'urgent', false, true, 'sent')`,
              [
                userId, // System sender
                studentId,
                `EMERGENCY ALERT: ${student.first_name} ${student.last_name}`,
                `Emergency alert received from ${student.first_name} ${student.last_name} (${student.email}).\n\n` +
                `Alert Type: ${finalAlertType.toUpperCase()}\n` +
                `Message: ${message || 'Emergency assistance needed'}\n\n` +
                `Please respond immediately.`
              ]
            );
          } catch (commError) {
            console.error(`Error creating staff notification for ${staff.email}:`, commError);
          }
        }

        // Log audit event (use school-specific database)
        await logAuditEvent(userId, req.user.role, 'alert_created', 'alert', alert.id, {
          alertType: finalAlertType,
          message,
          staffNotified: staffResult.rows.length,
          suicideRiskScreening: suicideRiskScreening ? 'completed' : 'not_completed'
        }, schoolPool);

        // Broadcast real-time alert to staff via WebSocket
        const io = req.app.get('io');
        if (io) {
          // Broadcast to all staff roles
          ['associate', 'expert', 'admin', 'staff'].forEach(role => {
            io.to(`role:${role}`).emit('new_alert', {
              alert: {
                id: alert.id,
                studentId: alert.student_id,
                studentName: `${student.first_name} ${student.last_name}`,
                alertType: alert.alert_type,
                status: alert.status,
                message: alert.message,
                createdAt: alert.created_at
              },
              timestamp: new Date().toISOString()
            });
          });

          // Also send to specific student
          io.to(`user:${userId}`).emit('alert_created', {
            alert: {
              id: alert.id,
              studentId: alert.student_id,
              alertType: alert.alert_type,
              status: alert.status,
              message: alert.message,
              createdAt: alert.created_at
            },
            timestamp: new Date().toISOString()
          });
        }

        res.json({
          success: true,
          alert: {
            id: alert.id,
            studentId: alert.student_id,
            alertType: alert.alert_type,
            status: alert.status,
            message: alert.message,
            createdAt: alert.created_at
          },
          staffNotified: staffResult.rows.length
        });
      } catch (error) {
        console.error('Create emergency alert error:', error);
        res.status(500).json({ error: 'Server error', details: error.message });
      }
    });

    // Get active alerts (associates/experts/staff - matches Mindaigle structure)
    router.get('/emergency-alerts', authenticateToken, requireRole('associate', 'expert', 'staff'), routeToSchoolDatabase, async (req, res) => {
      try {
        const { status } = req.query;
        const schoolName = req.user.schoolName;
        
        // Get the correct database pool for this school (from middleware or fallback)
        const schoolPool = req.db || (schoolName ? getPool(schoolName) : pool);
        
        if (!schoolName) {
          return res.status(400).json({ error: 'School not identified. Please log in again.' });
        }
        
        const statusFilter = status && ['active', 'acknowledged', 'resolved', 'cancelled'].includes(status) 
          ? status 
          : null;

        let query = `
      SELECT ea.*, 
             s.first_name || ' ' || s.last_name as student_name,
             s.grade,
             u.email as student_email,
             COALESCE(ua.first_name || ' ' || ua.last_name, ua.first_name, ua.last_name) as acknowledged_by_name,
             COALESCE(ur.first_name || ' ' || ur.last_name, ur.first_name, ur.last_name) as resolved_by_name
      FROM emergency_alerts ea
      JOIN students s ON ea.student_id = s.id
      JOIN users u ON s.user_id = u.id
      LEFT JOIN users ua ON ea.acknowledged_by = ua.id
      LEFT JOIN users ur ON ea.resolved_by = ur.id
    `;

        const params = [];
        if (statusFilter) {
          params.push(statusFilter);
          query += ` WHERE ea.status = $1`;
        }

        query += ` ORDER BY 
      CASE ea.alert_type 
        WHEN 'emergency' THEN 1 
        WHEN 'urgent' THEN 2 
        WHEN 'support' THEN 3 
      END,
      ea.created_at DESC`;

        const result = await schoolPool.query(query, params);

        const alerts = result.rows.map(row => ({
          id: row.id,
          studentId: row.student_id,
          studentName: row.student_name,
          studentEmail: row.student_email,
          grade: row.grade,
          alertType: row.alert_type,
          status: row.status,
          message: row.message,
          createdAt: row.created_at,
          updatedAt: row.updated_at,
          acknowledgedBy: row.acknowledged_by,
          acknowledgedByName: row.acknowledged_by_name,
          acknowledgedAt: row.acknowledged_at,
          resolvedBy: row.resolved_by,
          resolvedByName: row.resolved_by_name,
          resolvedAt: row.resolved_at,
          resolutionNotes: row.resolution_notes
        }));

        res.json({ alerts });
      } catch (error) {
        console.error('Get emergency alerts error:', error);
        res.status(500).json({ error: 'Server error', details: error.message });
      }
    });

// Acknowledge alert
router.put('/emergency-alert/:alertId/acknowledge', authenticateToken, requireRole('associate', 'expert', 'staff'), routeToSchoolDatabase, async (req, res) => {
  try {
    const { alertId } = req.params;
    const userId = req.user.userId;
    const schoolName = req.user.schoolName;

    // Get the correct database pool for this school (from middleware or fallback)
    const schoolPool = req.db || (schoolName ? getPool(schoolName) : pool);
    
    if (!schoolName) {
      return res.status(400).json({ error: 'School not identified. Please log in again.' });
    }

    // Get user name
    const userResult = await schoolPool.query(
      'SELECT first_name, last_name FROM users WHERE id = $1',
      [userId]
    );
    const user = userResult.rows[0];
    const userName = user ? `${user.first_name || ''} ${user.last_name || ''}`.trim() || 'Staff' : 'Staff';

    const result = await schoolPool.query(
      `UPDATE emergency_alerts 
       SET status = 'acknowledged',
           acknowledged_by = $1,
           acknowledged_at = CURRENT_TIMESTAMP
       WHERE id = $2 AND status = 'active'
       RETURNING *`,
      [userId, alertId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Alert not found or already processed' });
    }

    await logAuditEvent(userId, req.user.role, 'alert_acknowledged', 'alert', alertId, {}, schoolPool);

    res.json({
      success: true,
      alert: {
        id: result.rows[0].id,
        status: result.rows[0].status,
        acknowledgedBy: userId,
        acknowledgedByName: userName,
        acknowledgedAt: result.rows[0].acknowledged_at
      }
    });
  } catch (error) {
    console.error('Acknowledge alert error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Resolve alert
router.put('/emergency-alert/:alertId/resolve', authenticateToken, requireRole('associate', 'expert', 'staff'), routeToSchoolDatabase, async (req, res) => {
  try {
    const { alertId } = req.params;
    const { resolutionNotes } = req.body;
    const userId = req.user.userId;
    const schoolName = req.user.schoolName;

    // Get the correct database pool for this school (from middleware or fallback)
    const schoolPool = req.db || (schoolName ? getPool(schoolName) : pool);
    
    if (!schoolName) {
      return res.status(400).json({ error: 'School not identified. Please log in again.' });
    }

    // Get user name
    const userResult = await schoolPool.query(
      'SELECT first_name, last_name FROM users WHERE id = $1',
      [userId]
    );
    const user = userResult.rows[0];
    const userName = user ? `${user.first_name || ''} ${user.last_name || ''}`.trim() || 'Staff' : 'Staff';

    const result = await schoolPool.query(
      `UPDATE emergency_alerts 
       SET status = 'resolved',
           resolved_by = $1,
           resolved_at = CURRENT_TIMESTAMP,
           resolution_notes = $2
       WHERE id = $3 AND status IN ('active', 'acknowledged')
       RETURNING *`,
      [userId, resolutionNotes || '', alertId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Alert not found or already resolved' });
    }

    await logAuditEvent(userId, req.user.role, 'alert_resolved', 'alert', alertId, {
      resolutionNotes
    }, schoolPool);

    res.json({
      success: true,
      alert: {
        id: result.rows[0].id,
        status: result.rows[0].status,
        resolvedBy: userId,
        resolvedByName: userName,
        resolvedAt: result.rows[0].resolved_at,
        resolutionNotes: result.rows[0].resolution_notes
      }
    });
  } catch (error) {
    console.error('Resolve alert error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Get suicide-risk screening for an alert
router.get('/emergency-alert/:alertId/screening', authenticateToken, requireRole('associate', 'expert', 'staff', 'admin'), routeToSchoolDatabase, async (req, res) => {
  try {
    const { alertId } = req.params;
    const schoolName = req.user.schoolName;
    const schoolPool = req.db || (schoolName ? getPool(schoolName) : pool); // Use school-specific database from middleware
    
    const result = await schoolPool.query(
      `SELECT srs.*, 
              s.first_name || ' ' || s.last_name as student_name
       FROM suicide_risk_screenings srs
       JOIN students s ON srs.student_id = s.id
       WHERE srs.emergency_alert_id = $1
       ORDER BY srs.screening_completed_at DESC
       LIMIT 1`,
      [alertId]
    );
    
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Screening not found for this alert' });
    }
    
    const screening = result.rows[0];
    res.json({
      id: screening.id,
      studentId: screening.student_id,
      studentName: screening.student_name,
      emergencyAlertId: screening.emergency_alert_id,
      screeningQuestions: screening.screening_questions,
      riskScore: screening.risk_score,
      riskLevel: screening.risk_level,
      immediateActionRequired: screening.immediate_action_required,
      screeningCompletedAt: screening.screening_completed_at
    });
  } catch (error) {
    console.error('Get suicide-risk screening error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

module.exports = router;

