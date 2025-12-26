const express = require('express');
const pool = require('../config/database');
const { authenticateToken, requireRole } = require('../middleware/auth');

const router = express.Router();

// Create assistance request (parents only)
router.post('/assistance-request', authenticateToken, requireRole('parent'), async (req, res) => {
  try {
    const { studentId, message, urgency } = req.body;
    const parentId = req.user.userId;

    // Validate urgency
    if (!['low', 'normal', 'high'].includes(urgency)) {
      return res.status(400).json({ error: 'Invalid urgency level' });
    }

    // Verify parent is linked to student
    const linkResult = await pool.query(
      'SELECT id FROM parent_child_links WHERE parent_id = $1 AND student_id = $2 AND status = $3',
      [parentId, studentId, 'active']
    );

    if (linkResult.rows.length === 0) {
      return res.status(403).json({ error: 'Not linked to this student' });
    }

    // Get parent name
    const userResult = await pool.query(
      'SELECT name FROM users WHERE id = $1',
      [parentId]
    );
    const parentName = userResult.rows[0]?.name || 'Parent';

    // Create request
    const result = await pool.query(
      `INSERT INTO assistance_requests 
       (student_id, parent_id, parent_name, message, urgency, status)
       VALUES ($1, $2, $3, $4, $5, 'pending')
       RETURNING id`,
      [studentId, parentId, parentName, message, urgency]
    );

    res.json({
      success: true,
      requestId: result.rows[0].id
    });
  } catch (error) {
    console.error('Create assistance request error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Get assistance requests (associates/experts only)
router.get('/assistance-requests', authenticateToken, requireRole('associate', 'expert'), async (req, res) => {
  try {
    const result = await pool.query(
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

    const requests = result.rows.map(row => ({
      id: row.id,
      studentId: row.student_id,
      studentName: row.student_name,
      grade: row.grade,
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
    }));

    res.json({ requests });
  } catch (error) {
    console.error('Get assistance requests error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Update assistance request (associates/experts only)
router.put('/assistance-request/:requestId', authenticateToken, requireRole('associate', 'expert'), async (req, res) => {
  try {
    const { requestId } = req.params;
    const { status, notes } = req.body;
    const userId = req.user.userId;

    // Validate status
    if (status && !['pending', 'in-progress', 'resolved'].includes(status)) {
      return res.status(400).json({ error: 'Invalid status' });
    }

    // Get user name
    const userResult = await pool.query(
      'SELECT name FROM users WHERE id = $1',
      [userId]
    );
    const userName = userResult.rows[0]?.name || 'Staff';

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

    const result = await pool.query(
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

// Get parent's children
router.get('/parent/children', authenticateToken, requireRole('parent'), async (req, res) => {
  try {
    const parentId = req.user.userId;

    const result = await pool.query(
      `SELECT s.*, u.email, u.name as user_name,
       (SELECT emotion FROM daily_checkins WHERE student_id = s.id ORDER BY date DESC LIMIT 1) as recent_emotion,
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
      recentEmotion: row.recent_emotion,
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

