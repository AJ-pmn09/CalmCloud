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

// Get student's appointments
router.get('/appointments', authenticateToken, requireRole('student'), routeToSchoolDatabase, async (req, res) => {
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
      console.warn(`[appointments] No student profile for user_id=${userId}; returning empty list. Add a students row linked to this user.`);
      return res.json({ appointments: [] });
    }

    const studentId = studentResult.rows[0].id;

    // Get all appointments for this student
    const result = await schoolPool.query(
      `SELECT a.*, 
              u.first_name || ' ' || u.last_name as staff_name,
              u.email as staff_email,
              u.role as staff_role
       FROM appointments a
       LEFT JOIN users u ON a.staff_id = u.id
       WHERE a.student_id = $1
       ORDER BY a.appointment_date DESC`,
      [studentId]
    );

    const appointments = result.rows.map(row => ({
      id: row.id,
      studentId: row.student_id,
      staffId: row.staff_id,
      staffName: row.staff_name,
      staffEmail: row.staff_email,
      staffRole: row.staff_role,
      appointmentDate: row.appointment_date,
      duration: row.duration,
      type: row.type,
      notes: row.notes,
      status: row.status,
      createdAt: row.created_at,
      updatedAt: row.updated_at
    }));

    res.json({ appointments });
  } catch (error) {
    console.error('Get appointments error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Create new appointment
router.post('/appointments', authenticateToken, requireRole('student'), routeToSchoolDatabase, async (req, res) => {
  try {
    const { staffId, appointmentDate, duration, type, notes } = req.body;
    const userId = req.user.userId;
    const schoolName = req.user.schoolName;
    const schoolPool = req.db || (schoolName ? getPool(schoolName) : null);
    
    if (!schoolPool) {
      return res.status(500).json({ error: 'Database connection error' });
    }

    if (!appointmentDate) {
      return res.status(400).json({ error: 'Appointment date is required' });
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

    // Validate staff exists if provided
    if (staffId) {
      const staffResult = await schoolPool.query(
        'SELECT id, role FROM users WHERE id = $1 AND role IN ($2, $3, $4)',
        [staffId, 'associate', 'expert', 'staff']
      );

      if (staffResult.rows.length === 0) {
        return res.status(400).json({ error: 'Invalid staff member' });
      }
    }

    // Create appointment
    const result = await schoolPool.query(
      `INSERT INTO appointments 
       (student_id, staff_id, appointment_date, duration, type, notes, status, created_by)
       VALUES ($1, $2, $3, $4, $5, $6, 'pending', $7)
       RETURNING *`,
      [
        studentId,
        staffId || null,
        appointmentDate,
        duration || 30,
        type || 'general',
        notes || null,
        userId
      ]
    );

    const appointment = result.rows[0];

    // Log audit event
    await logAuditEvent(userId, req.user.role, 'appointment_created', 'appointment', appointment.id, {
      staffId,
      appointmentDate,
      type
    }, schoolPool);

    // Notify staff if assigned
    if (staffId) {
      // Create notification in communications table
      try {
        await schoolPool.query(
          `INSERT INTO communications 
           (sender_id, sender_role, recipient_type, recipient_id, subject, message, priority, status)
           VALUES ($1, 'student', 'student', $2, $3, $4, 'normal', 'sent')`,
          [
            userId,
            studentId,
            `New Appointment Request - ${appointmentDate}`,
            `Student has requested an appointment on ${appointmentDate}. Type: ${type || 'general'}`
          ]
        );
      } catch (commError) {
        console.error('Error creating appointment notification:', commError);
      }
    }

    res.json({
      success: true,
      appointment: {
        id: appointment.id,
        studentId: appointment.student_id,
        staffId: appointment.staff_id,
        appointmentDate: appointment.appointment_date,
        duration: appointment.duration,
        type: appointment.type,
        notes: appointment.notes,
        status: appointment.status,
        createdAt: appointment.created_at
      }
    });
  } catch (error) {
    console.error('Create appointment error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Get available staff for appointments
router.get('/appointments/staff-availability', authenticateToken, requireRole('student'), routeToSchoolDatabase, async (req, res) => {
  try {
    const schoolName = req.user.schoolName;
    const schoolPool = req.db || (schoolName ? getPool(schoolName) : null);
    
    if (!schoolPool) {
      return res.status(500).json({ error: 'Database connection error' });
    }

    // Get all available staff (associates, experts, staff)
    const result = await schoolPool.query(
      `SELECT u.id, u.first_name, u.last_name, u.email, u.role
       FROM users u
       WHERE u.role IN ('associate', 'expert', 'staff', 'admin')
       ORDER BY 
         CASE u.role
           WHEN 'admin' THEN 1
           WHEN 'expert' THEN 2
           WHEN 'associate' THEN 3
           WHEN 'staff' THEN 4
         END,
         u.first_name, u.last_name`
    );

    const staff = result.rows.map(row => ({
      id: row.id,
      name: `${row.first_name || ''} ${row.last_name || ''}`.trim() || row.email,
      email: row.email,
      role: row.role
    }));

    res.json({ staff });
  } catch (error) {
    console.error('Get staff availability error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Update appointment
router.put('/appointments/:id', authenticateToken, requireRole('student'), routeToSchoolDatabase, async (req, res) => {
  try {
    const { id } = req.params;
    const { appointmentDate, duration, type, notes } = req.body;
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

    // Check if appointment belongs to student
    const checkResult = await schoolPool.query(
      'SELECT * FROM appointments WHERE id = $1 AND student_id = $2',
      [id, studentId]
    );

    if (checkResult.rows.length === 0) {
      return res.status(404).json({ error: 'Appointment not found' });
    }

    // Update appointment (only if status is pending)
    if (checkResult.rows[0].status !== 'pending') {
      return res.status(400).json({ error: 'Can only update pending appointments' });
    }

    const updateFields = [];
    const updateValues = [];
    let paramIndex = 1;

    if (appointmentDate) {
      updateFields.push(`appointment_date = $${paramIndex++}`);
      updateValues.push(appointmentDate);
    }
    if (duration) {
      updateFields.push(`duration = $${paramIndex++}`);
      updateValues.push(duration);
    }
    if (type) {
      updateFields.push(`type = $${paramIndex++}`);
      updateValues.push(type);
    }
    if (notes !== undefined) {
      updateFields.push(`notes = $${paramIndex++}`);
      updateValues.push(notes);
    }

    if (updateFields.length === 0) {
      return res.status(400).json({ error: 'No fields to update' });
    }

    updateValues.push(id);
    const query = `UPDATE appointments SET ${updateFields.join(', ')}, updated_at = CURRENT_TIMESTAMP WHERE id = $${paramIndex} RETURNING *`;
    
    const result = await schoolPool.query(query, updateValues);
    const appointment = result.rows[0];

    // Log audit event
    await logAuditEvent(userId, req.user.role, 'appointment_updated', 'appointment', appointment.id, {
      appointmentDate,
      duration,
      type
    }, schoolPool);

    res.json({
      success: true,
      appointment: {
        id: appointment.id,
        studentId: appointment.student_id,
        staffId: appointment.staff_id,
        appointmentDate: appointment.appointment_date,
        duration: appointment.duration,
        type: appointment.type,
        notes: appointment.notes,
        status: appointment.status,
        updatedAt: appointment.updated_at
      }
    });
  } catch (error) {
    console.error('Update appointment error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Cancel appointment
router.delete('/appointments/:id', authenticateToken, requireRole('student'), routeToSchoolDatabase, async (req, res) => {
  try {
    const { id } = req.params;
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

    // Check if appointment belongs to student
    const checkResult = await schoolPool.query(
      'SELECT * FROM appointments WHERE id = $1 AND student_id = $2',
      [id, studentId]
    );

    if (checkResult.rows.length === 0) {
      return res.status(404).json({ error: 'Appointment not found' });
    }

    // Update status to cancelled instead of deleting
    const result = await schoolPool.query(
      `UPDATE appointments 
       SET status = 'cancelled', updated_at = CURRENT_TIMESTAMP 
       WHERE id = $1 
       RETURNING *`,
      [id]
    );

    const appointment = result.rows[0];

    // Log audit event
    await logAuditEvent(userId, req.user.role, 'appointment_cancelled', 'appointment', appointment.id, {}, schoolPool);

    res.json({
      success: true,
      message: 'Appointment cancelled successfully'
    });
  } catch (error) {
    console.error('Cancel appointment error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

module.exports = router;
