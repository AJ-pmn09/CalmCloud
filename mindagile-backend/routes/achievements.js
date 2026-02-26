const express = require('express');
const { authenticateToken, requireRole } = require('../middleware/auth');
const { routeToSchoolDatabase } = require('../middleware/databaseRouter');
const { getPool } = require('../config/databaseManager');

const router = express.Router();

// Note: Achievements are now computed dynamically from activity logs (matches dashboard logic)
// These endpoints are kept for backward compatibility but are not required

// Get all available achievements (optional - for reference)
router.get('/achievements', authenticateToken, requireRole('student'), routeToSchoolDatabase, async (req, res) => {
  try {
    // Return empty - achievements are computed from activity logs
    res.json({ achievements: [] });
  } catch (error) {
    console.error('Get achievements error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Get student's achievements (optional - computed on frontend)
router.get('/achievements/my-achievements', authenticateToken, requireRole('student'), routeToSchoolDatabase, async (req, res) => {
  try {
    // Return empty - achievements are computed from activity logs on frontend
    res.json({ achievements: [] });
  } catch (error) {
    console.error('Get my achievements error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

module.exports = router;
