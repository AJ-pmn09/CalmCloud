const { getPoolBySchoolId, getSchoolName } = require('../config/databaseManager');

/**
 * Middleware to route requests to the correct school database
 * Attaches the appropriate database pool to req.db based on user's school
 */
function routeToSchoolDatabase(req, res, next) {
  try {
    // Since all databases have school_id = 1, we use schoolName from JWT token for routing
    const schoolName = req.user?.schoolName;
    const schoolId = req.user?.schoolId;
    
    // Try to get school name from token (preferred method)
    if (schoolName) {
      try {
        const { getPool } = require('../config/databaseManager');
        req.db = getPool(schoolName);
        req.schoolId = schoolId || 1;
        req.schoolName = schoolName;
        next();
        return;
      } catch (error) {
        console.error('Database routing error:', error);
        return res.status(500).json({ 
          error: 'Database connection error',
          details: process.env.NODE_ENV !== 'production' ? error.message : undefined
        });
      }
    }
    
    // Fallback: Try to use school_id (though all are 1, this won't work well)
    if (schoolId) {
      const schoolNameFromId = getSchoolName(schoolId);
      if (schoolNameFromId) {
        try {
          req.db = getPoolBySchoolId(schoolId);
          req.schoolId = schoolId;
          req.schoolName = schoolNameFromId;
          next();
          return;
        } catch (error) {
          console.error('Database routing error:', error);
        }
      }
    }
    
    // If we get here, school information is missing
    const userId = req.user?.userId;
    if (!userId) {
      return res.status(401).json({ error: 'Authentication required' });
    }
    
    return res.status(400).json({ 
      error: 'School not identified. Please log in again.',
      details: 'Your session does not include school information.'
    });
  } catch (error) {
    console.error('Database router middleware error:', error);
    return res.status(500).json({ 
      error: 'Server error',
      details: process.env.NODE_ENV !== 'production' ? error.message : undefined
    });
  }
}

/**
 * Optional: Middleware for routes that don't need school-specific database
 * (e.g., authentication, health checks)
 */
function skipDatabaseRouting(req, res, next) {
  // Just pass through without setting req.db
  next();
}

module.exports = {
  routeToSchoolDatabase,
  skipDatabaseRouting
};

