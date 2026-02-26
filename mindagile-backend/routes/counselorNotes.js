const express = require('express');
const { authenticateToken, requireRole } = require('../middleware/auth');
const { routeToSchoolDatabase } = require('../middleware/databaseRouter');
const { getPool } = require('../config/databaseManager');

const router = express.Router();

// Get counselor notes for student
router.get('/counselor-notes', authenticateToken, requireRole('student'), routeToSchoolDatabase, async (req, res) => {
  try {
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

    // Get counselor notes (note_text + note_json for NLP when column exists)
    const result = await schoolPool.query(
      `SELECT cn.*, 
              u.first_name || ' ' || u.last_name as counselor_name,
              u.email as counselor_email,
              u.role as counselor_role
       FROM counselor_notes cn
       LEFT JOIN users u ON cn.counselor_id = u.id
       WHERE cn.student_id = $1
       ORDER BY cn.created_at DESC`,
      [studentId]
    );

    const notes = result.rows.map(row => ({
      id: row.id,
      studentId: row.student_id,
      counselorId: row.counselor_id,
      counselorName: row.counselor_name,
      counselorEmail: row.counselor_email,
      counselorRole: row.counselor_role,
      noteText: row.note_text,
      noteJson: row.note_json || null,
      createdAt: row.created_at,
      updatedAt: row.updated_at
    }));

    res.json({ notes });
  } catch (error) {
    console.error('Get counselor notes error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

module.exports = router;
