const express = require('express');
const { authenticateToken, requireRole } = require('../middleware/auth');
const { routeToSchoolDatabase } = require('../middleware/databaseRouter');
const { getPool } = require('../config/databaseManager');

const router = express.Router();

// Helper function to log audit events
async function logAuditEvent(userId, userRole, actionType, resourceType, resourceId, details = {}, schoolPool = null) {
  try {
    const dbPool = schoolPool || getPool('horizons'); // Fallback
    await dbPool.query(
      `INSERT INTO audit_logs (user_id, user_role, action_type, resource_type, resource_id, details)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [userId, userRole, actionType, resourceType, resourceId, JSON.stringify(details)]
    );
  } catch (error) {
    console.error('Audit log error:', error);
  }
}

// Create assistance request (parents only) - supports multiple children - using school-specific database
router.post('/assistance-request', authenticateToken, requireRole('parent'), routeToSchoolDatabase, async (req, res) => {
  try {
    const { studentIds, message, urgency } = req.body;
    const parentId = req.user.userId;
    const schoolName = req.user.schoolName;
    const schoolPool = req.db || (schoolName ? getPool(schoolName) : null);
    
    if (!schoolPool) {
      return res.status(500).json({ error: 'Database connection error' });
    }

    // Validate urgency
    if (!['low', 'normal', 'high'].includes(urgency)) {
      return res.status(400).json({ error: 'Invalid urgency level' });
    }

    // Validate studentIds - can be array, single ID, or "all"
    let targetStudentIds = [];
    if (studentIds === 'all' || studentIds === null || studentIds === undefined) {
      // Get all linked children
      const allChildrenResult = await schoolPool.query(
        'SELECT student_id FROM parent_child_links WHERE parent_id = $1 AND status = $2',
        [parentId, 'active']
      );
      targetStudentIds = allChildrenResult.rows.map(row => row.student_id);
    } else if (Array.isArray(studentIds)) {
      targetStudentIds = studentIds;
    } else if (typeof studentIds === 'number' || typeof studentIds === 'string') {
      targetStudentIds = [parseInt(studentIds)];
    }

    if (targetStudentIds.length === 0) {
      return res.status(400).json({ error: 'No valid students selected' });
    }

    // Verify parent is linked to all selected students
    const linkResult = await schoolPool.query(
      `SELECT student_id FROM parent_child_links 
       WHERE parent_id = $1 AND student_id = ANY($2::int[]) AND status = $3`,
      [parentId, targetStudentIds, 'active']
    );

    const linkedStudentIds = linkResult.rows.map(row => row.student_id);
    if (linkedStudentIds.length !== targetStudentIds.length) {
      return res.status(403).json({ error: 'Not linked to one or more selected students' });
    }

    // Get parent name
    const userResult = await schoolPool.query(
      'SELECT first_name, last_name FROM users WHERE id = $1',
      [parentId]
    );
    const user = userResult.rows[0];
    const parentName = user ? `${user.first_name || ''} ${user.last_name || ''}`.trim() || 'Parent' : 'Parent';

    // Create a request for the primary student (first one) and link others
    const primaryStudentId = targetStudentIds[0];
    const result = await schoolPool.query(
      `INSERT INTO assistance_requests 
       (student_id, parent_id, parent_name, message, urgency, status)
       VALUES ($1, $2, $3, $4, $5, 'pending')
       RETURNING id`,
      [primaryStudentId, parentId, parentName, message, urgency]
    );

    const requestId = result.rows[0].id;

    // Link all selected children to this request (using assistance_request_students per Mindaigle)
    for (const studentId of targetStudentIds) {
      await schoolPool.query(
        `INSERT INTO assistance_request_students (assistance_request_id, student_id)
         VALUES ($1, $2)
         ON CONFLICT DO NOTHING`,
        [requestId, studentId]
      );
    }

    // Log audit event
    await logAuditEvent(parentId, req.user.role, 'request_created', 'request', requestId, {
      studentIds: targetStudentIds,
      urgency,
      messageLength: message.length
    }, schoolPool);

    res.json({
      success: true,
      requestId: requestId,
      studentIds: targetStudentIds
    });
  } catch (error) {
    console.error('Create assistance request error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Get assistance requests (associates/experts only) - using school-specific database
router.get('/assistance-requests', authenticateToken, requireRole('associate', 'expert'), routeToSchoolDatabase, async (req, res) => {
  try {
    const schoolName = req.user.schoolName;
    const schoolPool = req.db || (schoolName ? getPool(schoolName) : null);
    
    if (!schoolPool) {
      return res.status(500).json({ error: 'Database connection error' });
    }

    const result = await schoolPool.query(
      `SELECT ar.*, 
       s.first_name || ' ' || s.last_name as student_name,
       s.grade
       FROM assistance_requests ar
       JOIN students s ON ar.student_id = s.id
       ORDER BY 
         CASE urgency 
           WHEN 'high' THEN 1 
           WHEN 'normal' THEN 2 
           WHEN 'low' THEN 3 
         END,
         ar.created_at DESC`
    );

    console.log(`[Assistance Requests] School: ${schoolName}, Found ${result.rows.length} requests`);

    // Get all linked children for each request (using assistance_request_students per Mindaigle)
    const requests = await Promise.all(result.rows.map(async (row) => {
      const childrenResult = await schoolPool.query(
        `SELECT ars.student_id, 
                s.first_name || ' ' || s.last_name as student_name,
                s.grade
         FROM assistance_request_students ars
         JOIN students s ON ars.student_id = s.id
         WHERE ars.assistance_request_id = $1`,
        [row.id]
      );

      return {
        id: row.id,
        studentId: row.student_id,
        studentName: row.student_name,
        grade: row.grade,
        studentIds: childrenResult.rows.map(r => r.student_id),
        studentNames: childrenResult.rows.map(r => r.student_name),
        parentId: row.parent_id,
        parentName: row.parent_name,
        message: row.message,
        urgency: row.urgency,
        status: row.status,
        handledBy: row.handled_by,
        handledByName: row.handled_by_name,
        handledAt: row.handled_at,
        notes: row.notes,
        createdAt: row.created_at
      };
    }));

    res.json({ requests });
  } catch (error) {
    console.error('Get assistance requests error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Update assistance request (associates/experts only) - using school-specific database
router.put('/assistance-request/:requestId', authenticateToken, requireRole('associate', 'expert'), routeToSchoolDatabase, async (req, res) => {
  try {
    const { requestId } = req.params;
    const { status, notes } = req.body;
    const userId = req.user.userId;
    const schoolName = req.user.schoolName;
    const schoolPool = req.db || (schoolName ? getPool(schoolName) : null);
    
    if (!schoolPool) {
      return res.status(500).json({ error: 'Database connection error' });
    }

    // Validate status
    if (status && !['pending', 'in-progress', 'resolved'].includes(status)) {
      return res.status(400).json({ error: 'Invalid status' });
    }

    // Get user name
    const userResult = await schoolPool.query(
      'SELECT first_name, last_name FROM users WHERE id = $1',
      [userId]
    );
    const user = userResult.rows[0];
    const userName = user ? `${user.first_name || ''} ${user.last_name || ''}`.trim() || 'Staff' : 'Staff';

    // Update request
    const updateFields = [];
    const updateValues = [];
    let paramCount = 1;

    if (status) {
      updateFields.push(`status = $${paramCount++}`);
      updateValues.push(status);
    }

    if (notes !== undefined) {
      updateFields.push(`notes = $${paramCount++}`);
      updateValues.push(notes);
    }

    if (status && status !== 'pending') {
      updateFields.push(`handled_by = $${paramCount++}`);
      updateValues.push(userId);
      updateFields.push(`handled_by_name = $${paramCount++}`);
      updateValues.push(userName);
      updateFields.push(`handled_at = $${paramCount++}`);
      updateValues.push(new Date().toISOString());
    }

    updateFields.push(`updated_at = $${paramCount++}`);
    updateValues.push(new Date().toISOString());

    updateValues.push(requestId);

    const result = await schoolPool.query(
      `UPDATE assistance_requests 
       SET ${updateFields.join(', ')}
       WHERE id = $${paramCount}
       RETURNING *`,
      updateValues
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Request not found' });
    }

    const request = result.rows[0];

    // Log audit event
    await logAuditEvent(userId, req.user.role, 'request_updated', 'request', requestId, {
      status,
      hasNotes: !!notes
    }, schoolPool);

    res.json({
      success: true,
      request: {
        id: request.id,
        status: request.status,
        notes: request.notes,
        handledBy: request.handled_by,
        handledByName: request.handled_by_name,
        handledAt: request.handled_at
      }
    });
  } catch (error) {
    console.error('Update assistance request error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Get parent's children (use school DB when available)
router.get('/parent/children', authenticateToken, requireRole('parent'), routeToSchoolDatabase, async (req, res) => {
  try {
    const parentId = req.user.userId;
    const schoolPool = req.db || (req.user?.schoolName ? getPool(req.user.schoolName) : null) || pool;

    const result = await schoolPool.query(
      `SELECT s.*, u.email, 
       COALESCE(TRIM(u.first_name || ' ' || u.last_name), u.first_name, u.last_name, 'User') as user_name,
       (SELECT mood_rating FROM daily_checkins WHERE student_id = s.id ORDER BY date DESC LIMIT 1) as recent_mood_rating,
       (SELECT stress_level FROM daily_checkins WHERE student_id = s.id ORDER BY date DESC LIMIT 1) as recent_stress,
       (SELECT COUNT(*) FROM daily_checkins WHERE student_id = s.id) as checkin_count,
       (SELECT MAX(date) FROM daily_checkins WHERE student_id = s.id) as last_checkin_date
       FROM students s
       JOIN parent_child_links pcl ON s.id = pcl.student_id
       JOIN users u ON s.user_id = u.id
       WHERE pcl.parent_id = $1 AND pcl.status = 'active'
       ORDER BY s.first_name, s.last_name`,
      [parentId]
    );

    const children = result.rows.map(row => ({
      id: row.id,
      name: `${row.first_name} ${row.last_name}`,
      email: row.email,
      grade: row.grade,
      profilePictureUrl: row.profile_picture_url || null,
      recentEmotion: row.recent_mood_rating ? (row.recent_mood_rating === 5 ? 'happy' : row.recent_mood_rating === 4 ? 'calm' : row.recent_mood_rating === 3 ? 'okay' : row.recent_mood_rating === 2 ? 'anxious' : 'sad') : null,
      recentStress: row.recent_stress ? parseInt(row.recent_stress) : null,
      checkinCount: parseInt(row.checkin_count) || 0,
      lastCheckinDate: row.last_checkin_date
    }));

    res.json({ children });
  } catch (error) {
    console.error('Get parent children error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

module.exports = router;

