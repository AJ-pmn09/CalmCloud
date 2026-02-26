const express = require('express');
const pool = require('../config/database');
const { authenticateToken, requireRole } = require('../middleware/auth');
const { routeToSchoolDatabase } = require('../middleware/databaseRouter');

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

// Create communication table if it doesn't exist
async function ensureCommunicationsTable() {
  try {
    // Check if table exists
    const tableCheck = await pool.query(`
      SELECT EXISTS (
        SELECT FROM information_schema.tables 
        WHERE table_schema = 'public' 
        AND table_name = 'communications'
      )
    `);
    
    if (!tableCheck.rows[0].exists) {
      await pool.query(`
        CREATE TABLE communications (
          id SERIAL PRIMARY KEY,
          sender_id INTEGER REFERENCES users(id) ON DELETE CASCADE NOT NULL,
          sender_role VARCHAR(50) NOT NULL,
          recipient_type VARCHAR(50) NOT NULL CHECK (recipient_type IN ('student', 'cohort', 'all')),
          recipient_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
          recipient_cohort VARCHAR(255), -- e.g., 'grade_9', 'school_1', 'all_students'
          subject VARCHAR(255),
          message TEXT NOT NULL,
          priority VARCHAR(50) DEFAULT 'normal' CHECK (priority IN ('low', 'normal', 'high', 'urgent')),
          parent_visible BOOLEAN DEFAULT true, -- Whether parents can see this communication
          emergency_override BOOLEAN DEFAULT false, -- Emergency escalation override
          status VARCHAR(50) DEFAULT 'sent' CHECK (status IN ('draft', 'sent', 'read', 'archived')),
          read_at TIMESTAMP,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
      `);
      
      // Create indexes for faster queries
      await pool.query(`
        CREATE INDEX idx_communications_recipient 
        ON communications(recipient_type, recipient_id, recipient_cohort)
      `);
      
      await pool.query(`
        CREATE INDEX idx_communications_sender 
        ON communications(sender_id, created_at)
      `);
      
      console.log('Communications table created successfully');
    }
  } catch (error) {
    console.error('Error ensuring communications table:', error);
  }
}

// Initialize table on module load
ensureCommunicationsTable();

// Send message to individual student or broadcast to cohort
router.post('/send-message', authenticateToken, requireRole('associate', 'expert', 'staff', 'admin'), async (req, res) => {
  try {
    const { recipientType, recipientId, recipientCohort, subject, message, priority, parentVisible, emergencyOverride } = req.body;
    const userId = req.user.userId;
    const userRole = req.user.role;

    // Validate recipient type
    if (!['student', 'cohort', 'all'].includes(recipientType)) {
      return res.status(400).json({ error: 'Invalid recipient type. Must be student, cohort, or all' });
    }

    // Validate recipient for individual student
    if (recipientType === 'student' && !recipientId) {
      return res.status(400).json({ error: 'recipientId is required for individual student messages' });
    }

    // Validate cohort for cohort messages
    if (recipientType === 'cohort' && !recipientCohort) {
      return res.status(400).json({ error: 'recipientCohort is required for cohort messages' });
    }

    // Validate message
    if (!message || message.trim().length === 0) {
      return res.status(400).json({ error: 'Message is required' });
    }

    // If emergency override, ensure user has permission
    if (emergencyOverride && !['expert', 'admin'].includes(userRole)) {
      return res.status(403).json({ error: 'Only experts and admins can use emergency override' });
    }

    // Get sender name
    const senderResult = await pool.query(
      'SELECT first_name, last_name FROM users WHERE id = $1',
      [userId]
    );
    const sender = senderResult.rows[0];
    const senderName = sender ? `${sender.first_name || ''} ${sender.last_name || ''}`.trim() : 'Staff';

    // Determine actual recipients
    let actualRecipients = [];
    
    if (recipientType === 'student') {
      // Single student
      const studentResult = await pool.query(
        'SELECT id, first_name, last_name, user_id FROM students WHERE id = $1',
        [recipientId]
      );
      if (studentResult.rows.length === 0) {
        return res.status(404).json({ error: 'Student not found' });
      }
      actualRecipients = [studentResult.rows[0]];
    } else if (recipientType === 'cohort') {
      // Cohort: parse cohort identifier (e.g., 'grade_9', 'school_1')
      let cohortQuery = 'SELECT id, first_name, last_name, user_id FROM students WHERE 1=1';
      const cohortParams = [];
      let paramCount = 1;

      if (recipientCohort.startsWith('grade_')) {
        const grade = parseInt(recipientCohort.replace('grade_', ''));
        cohortQuery += ` AND grade = $${paramCount++}`;
        cohortParams.push(grade);
      } else if (recipientCohort.startsWith('school_')) {
        const schoolId = parseInt(recipientCohort.replace('school_', ''));
        cohortQuery += ` AND school_id = $${paramCount++}`;
        cohortParams.push(schoolId);
      } else if (recipientCohort === 'all_students') {
        // All students - no additional filter
      } else {
        return res.status(400).json({ error: 'Invalid cohort identifier. Use grade_X, school_X, or all_students' });
      }

      const cohortResult = await pool.query(cohortQuery, cohortParams);
      actualRecipients = cohortResult.rows;
    } else if (recipientType === 'all') {
      // All students
      const allResult = await pool.query(
        'SELECT id, first_name, last_name, user_id FROM students'
      );
      actualRecipients = allResult.rows;
    }

    if (actualRecipients.length === 0) {
      return res.status(404).json({ error: 'No recipients found' });
    }

    // Create communication records for each recipient (message + message_json for NLP)
    const communicationIds = [];
    for (const recipient of actualRecipients) {
      const msgJson = JSON.stringify({
        text: message,
        senderId: userId,
        senderRole: userRole,
        recipientType,
        recipientId: recipient.id,
        recipientCohort: recipientCohort || null,
        subject: subject || null,
        createdAt: new Date().toISOString(),
        source: 'communication'
      });
      let result;
      try {
        result = await pool.query(
          `INSERT INTO communications 
           (sender_id, sender_role, recipient_type, recipient_id, recipient_cohort, 
            subject, message, message_json, priority, parent_visible, emergency_override, status)
           VALUES ($1, $2, $3, $4, $5, $6, $7, $8::jsonb, $9, $10, $11, 'sent')
           RETURNING id`,
          [
            userId,
            userRole,
            recipientType,
            recipient.id,
            recipientCohort || null,
            subject || null,
            message,
            msgJson,
            priority || 'normal',
            parentVisible !== false,
            emergencyOverride || false
          ]
        );
      } catch (colErr) {
        if (colErr.message && colErr.message.includes('message_json')) {
          result = await pool.query(
            `INSERT INTO communications 
             (sender_id, sender_role, recipient_type, recipient_id, recipient_cohort, 
              subject, message, priority, parent_visible, emergency_override, status)
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, 'sent')
             RETURNING id`,
            [
              userId,
              userRole,
              recipientType,
              recipient.id,
              recipientCohort || null,
              subject || null,
              message,
              priority || 'normal',
              parentVisible !== false,
              emergencyOverride || false
            ]
          );
        } else throw colErr;
      }
      communicationIds.push(result.rows[0].id);

      // Log audit event for each communication
      await logAuditEvent(userId, userRole, 'communication_sent', 'communication', result.rows[0].id, {
        recipientType,
        recipientId: recipient.id,
        recipientName: `${recipient.first_name} ${recipient.last_name}`,
        recipientCohort,
        priority: priority || 'normal',
        parentVisible: parentVisible !== false,
        emergencyOverride: emergencyOverride || false
      });
    }

    res.json({
      success: true,
      message: `Message sent to ${actualRecipients.length} recipient(s)`,
      communicationIds,
      recipientCount: actualRecipients.length
    });
  } catch (error) {
    console.error('Send message error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Get communications for student
router.get('/my-messages', authenticateToken, requireRole('student'), routeToSchoolDatabase, async (req, res) => {
  try {
    const userId = req.user.userId;

    // Get student ID (using school-specific database)
    const studentResult = await req.db.query(
      'SELECT id FROM students WHERE user_id = $1',
      [userId]
    );

    if (studentResult.rows.length === 0) {
      console.warn(`[communications/my-messages] No student profile for user_id=${userId}; returning empty list. Add a students row linked to this user.`);
      return res.json({ communications: [] });
    }

    const studentId = studentResult.rows[0].id;

    // Get all communications for this student (individual, cohort, or all)
    // Note: communications table does NOT exist in actual database - return empty array
    let result;
    try {
      // Try school-specific database first
      // Handle case where users table might not have first_name/last_name
      result = await req.db.query(
      `SELECT c.*, 
              COALESCE(
                NULLIF(TRIM(COALESCE(u.first_name, '') || ' ' || COALESCE(u.last_name, '')), ''),
                u.email,
                'Staff'
              ) as sender_name,
              u.email as sender_email
       FROM communications c
       JOIN users u ON c.sender_id = u.id
       WHERE c.status = 'sent'
         AND (
           (c.recipient_type = 'student' AND c.recipient_id = $1)
           OR (c.recipient_type = 'cohort' AND (
             EXISTS (
               SELECT 1 FROM students s 
               WHERE s.id = $1 
               AND (
                 (c.recipient_cohort LIKE 'grade_%' AND s.grade = CAST(SUBSTRING(c.recipient_cohort FROM 'grade_(\\d+)') AS INTEGER))
                 OR (c.recipient_cohort LIKE 'school_%' AND s.school_id = CAST(SUBSTRING(c.recipient_cohort FROM 'school_(\\d+)') AS INTEGER))
                 OR c.recipient_cohort = 'all_students'
               )
             )
           ))
           OR (c.recipient_type = 'all')
         )
       ORDER BY c.created_at DESC`,
      [studentId]
      );
    } catch (commError) {
      // Communications table doesn't exist - return empty array
      if (commError.code === '42P01') {
        console.log('Communications table does not exist - returning empty array');
        return res.json({
          success: true,
          communications: []
        });
      }
      throw commError;
    }

    const communications = result.rows.map(row => ({
      id: row.id,
      senderId: row.sender_id,
      senderName: row.sender_name,
      senderEmail: row.sender_email,
      senderRole: row.sender_role,
      recipientType: row.recipient_type,
      subject: row.subject,
      message: row.message,
      priority: row.priority,
      parentVisible: row.parent_visible,
      emergencyOverride: row.emergency_override,
      status: row.status,
      readAt: row.read_at,
      createdAt: row.created_at
    }));

    res.json({ communications });
  } catch (error) {
    console.error('Get my messages error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Mark message as read
router.put('/message/:messageId/read', authenticateToken, requireRole('student'), async (req, res) => {
  try {
    const { messageId } = req.params;
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

    // Update message status
    const result = await pool.query(
      `UPDATE communications 
       SET status = 'read', read_at = CURRENT_TIMESTAMP
       WHERE id = $1 
         AND (
           (recipient_type = 'student' AND recipient_id = $2)
           OR (recipient_type IN ('cohort', 'all'))
         )
       RETURNING *`,
      [messageId, studentId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Message not found or access denied' });
    }

    await logAuditEvent(userId, req.user.role, 'communication_read', 'communication', messageId);

    res.json({ success: true, message: result.rows[0] });
  } catch (error) {
    console.error('Mark message as read error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Get sent communications (for staff)
router.get('/sent-messages', authenticateToken, requireRole('associate', 'expert', 'staff', 'admin'), async (req, res) => {
  try {
    const userId = req.user.userId;
    const { limit = 50, offset = 0 } = req.query;

    const result = await pool.query(
      `SELECT c.*, 
              COUNT(*) OVER() as total_count,
              s.first_name || ' ' || s.last_name as recipient_name
       FROM communications c
       LEFT JOIN students s ON c.recipient_id = s.id
       WHERE c.sender_id = $1
       ORDER BY c.created_at DESC
       LIMIT $2 OFFSET $3`,
      [userId, parseInt(limit), parseInt(offset)]
    );

    const communications = result.rows.map(row => ({
      id: row.id,
      recipientType: row.recipient_type,
      recipientId: row.recipient_id,
      recipientName: row.recipient_name,
      recipientCohort: row.recipient_cohort,
      subject: row.subject,
      message: row.message,
      priority: row.priority,
      parentVisible: row.parent_visible,
      emergencyOverride: row.emergency_override,
      status: row.status,
      createdAt: row.created_at,
      totalCount: parseInt(row.total_count)
    }));

    res.json({ communications, total: result.rows.length > 0 ? parseInt(result.rows[0].total_count) : 0 });
  } catch (error) {
    console.error('Get sent messages error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Get communications visible to parents
router.get('/parent-visible-messages/:studentId', authenticateToken, requireRole('parent'), async (req, res) => {
  try {
    const { studentId } = req.params;
    const userId = req.user.userId;

    // Verify parent has access to this student
    const linkResult = await pool.query(
      `SELECT 1 FROM parent_child_links 
       WHERE parent_id = $1 AND student_id = $2 AND status = 'active'`,
      [userId, studentId]
    );

    if (linkResult.rows.length === 0) {
      return res.status(403).json({ error: 'Access denied' });
    }

    // Get communications visible to parents
    const result = await pool.query(
      `SELECT c.*, 
              u.first_name || ' ' || u.last_name as sender_name
       FROM communications c
       JOIN users u ON c.sender_id = u.id
       WHERE c.parent_visible = true
         AND (
           (c.recipient_type = 'student' AND c.recipient_id = $1)
           OR (c.recipient_type IN ('cohort', 'all'))
         )
       ORDER BY c.created_at DESC`,
      [studentId]
    );

    const communications = result.rows.map(row => ({
      id: row.id,
      senderName: row.sender_name,
      senderRole: row.sender_role,
      subject: row.subject,
      message: row.message,
      priority: row.priority,
      createdAt: row.created_at
    }));

    res.json({ communications });
  } catch (error) {
    console.error('Get parent visible messages error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

module.exports = router;

