const express = require('express');
const { authenticateToken, requireRole } = require('../middleware/auth');
const { routeToSchoolDatabase } = require('../middleware/databaseRouter');
const { getPool } = require('../config/databaseManager');

const router = express.Router();

// Staff reply to student request/message
router.post('/communications/:communicationId/reply', authenticateToken, requireRole('associate', 'expert', 'staff', 'admin'), routeToSchoolDatabase, async (req, res) => {
  try {
    const { communicationId } = req.params;
    const { replyMessage, note } = req.body;
    const userId = req.user.userId;
    const userRole = req.user.role;
    const schoolName = req.user.schoolName;
    const schoolPool = req.db || (schoolName ? getPool(schoolName) : null);
    
    if (!schoolPool) {
      return res.status(500).json({ error: 'Database connection error' });
    }

    if (!replyMessage || replyMessage.trim().length === 0) {
      return res.status(400).json({ error: 'Reply message is required' });
    }

    // Get original communication (from student)
    const commResult = await schoolPool.query(
      `SELECT c.*, s.id as student_id, s.user_id as student_user_id
       FROM communications c
       JOIN students s ON c.sender_id = s.user_id
       WHERE c.id = $1 AND c.sender_role = 'student'`,
      [communicationId]
    );

    if (commResult.rows.length === 0) {
      return res.status(404).json({ error: 'Student request not found' });
    }

    const originalComm = commResult.rows[0];
    const studentId = originalComm.student_id;
    const studentUserId = originalComm.student_user_id;

    // Get staff name
    const staffResult = await schoolPool.query(
      `SELECT 
        COALESCE(
          NULLIF(TRIM(COALESCE(first_name, '') || ' ' || COALESCE(last_name, '')), ''),
          email,
          'Staff'
        ) as name,
        role
       FROM users WHERE id = $1`,
      [userId]
    );

    const staffName = staffResult.rows[0]?.name || 'Staff';
    const staffRole = staffResult.rows[0]?.role || userRole;

    // Create reply as a new communication
    const replyResult = await schoolPool.query(
      `INSERT INTO communications 
       (sender_id, sender_role, recipient_type, recipient_id, subject, message, priority, status)
       VALUES ($1, $2, 'student', $3, $4, $5, $6, 'sent')
       RETURNING *`,
      [
        userId,
        userRole,
        studentId,
        `Re: ${originalComm.subject || 'Your Request'}`,
        replyMessage,
        originalComm.priority || 'normal'
      ]
    );

    const reply = replyResult.rows[0];

    // If note is provided, also create a counselor note (note_text + note_json for NLP)
    if (note && note.trim().length > 0) {
      try {
        const noteJson = JSON.stringify({
          text: note,
          authorId: userId,
          authorRole: staffRole,
          authorName: staffName,
          createdAt: new Date().toISOString(),
          source: 'staff_reply',
          studentId
        });
        try {
          await schoolPool.query(
            `INSERT INTO counselor_notes 
             (student_id, counselor_id, note_text, note_json)
             VALUES ($1, $2, $3, $4::jsonb)`,
            [studentId, userId, note, noteJson]
          );
        } catch (colErr) {
          if (colErr.message && colErr.message.includes('note_json')) {
            await schoolPool.query(
              `INSERT INTO counselor_notes (student_id, counselor_id, note_text) VALUES ($1, $2, $3)`,
              [studentId, userId, note]
            );
          } else throw colErr;
        }
      } catch (noteError) {
        console.error('Error creating counselor note:', noteError);
        // Continue even if note creation fails
      }
    }

    // Log audit event
    try {
      await schoolPool.query(
        `INSERT INTO audit_logs (user_id, user_role, action_type, resource_type, resource_id, details)
         VALUES ($1, $2, 'communication_replied', 'communication', $3, $4)`,
        [
          userId,
          userRole,
          reply.id,
          JSON.stringify({
            originalCommunicationId: communicationId,
            studentId: studentId,
            hasNote: !!note
          })
        ]
      );
    } catch (auditError) {
      console.error('Error logging audit event:', auditError);
    }

    res.json({
      success: true,
      message: 'Reply sent successfully',
      reply: {
        id: reply.id,
        message: reply.message,
        createdAt: reply.created_at
      },
      noteCreated: !!note
    });
  } catch (error) {
    console.error('Staff reply error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Get student requests for staff to reply to
router.get('/student-requests', authenticateToken, requireRole('associate', 'expert', 'staff', 'admin'), routeToSchoolDatabase, async (req, res) => {
  try {
    const userId = req.user.userId;
    const schoolName = req.user.schoolName;
    const schoolPool = req.db || (schoolName ? getPool(schoolName) : null);
    
    if (!schoolPool) {
      return res.status(500).json({ error: 'Database connection error' });
    }

    // Get all communications from students (requests) that haven't been replied to
    // A communication is considered a "request" if it's from a student
    const result = await schoolPool.query(
      `SELECT c.*, 
              s.id as student_id,
              COALESCE(
                NULLIF(TRIM(COALESCE(s.first_name, '') || ' ' || COALESCE(s.last_name, '')), ''),
                u.email,
                'Student'
              ) as student_name,
              s.grade,
              u.email as student_email
       FROM communications c
       JOIN students s ON c.sender_id = s.user_id
       JOIN users u ON s.user_id = u.id
       WHERE c.sender_role = 'student'
         AND c.status = 'sent'
         AND NOT EXISTS (
           SELECT 1 FROM communications c2 
           WHERE c2.recipient_id = s.id 
             AND c2.sender_id = $1
             AND c2.subject LIKE 'Re: ' || COALESCE(c.subject, '')
         )
       ORDER BY c.created_at DESC
       LIMIT 50`,
      [userId]
    );

    const requests = result.rows.map(row => ({
      id: row.id,
      studentId: row.student_id,
      studentName: row.student_name,
      studentEmail: row.student_email,
      studentGrade: row.grade,
      subject: row.subject,
      message: row.message,
      priority: row.priority,
      emergencyOverride: row.emergency_override,
      createdAt: row.created_at
    }));

    res.json({ requests });
  } catch (error) {
    console.error('Get student requests error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

module.exports = router;
