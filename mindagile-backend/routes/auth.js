const express = require('express');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const path = require('path');
const fs = require('fs');
const pool = require('../config/database'); // Master DB for authentication
const { getAllPools, getPool, getSchoolName } = require('../config/databaseManager'); // For multi-DB user lookup and school routing
const { authenticateToken } = require('../middleware/auth');
const { JWT_SECRET: JWT_SECRET_FROM_AUTH } = require('../middleware/auth');
// Use JWT_SECRET from middleware (which has fallback to default)
const JWT_SECRET = JWT_SECRET_FROM_AUTH || process.env.JWT_SECRET || 'dev-secret-key-change-in-production';

const router = express.Router();

// Register new user
router.post('/signup', async (req, res) => {
  try {
    const { email, password, name, first_name, last_name, role, studentEmail } = req.body;

    // Validate role
    const validRoles = ['parent', 'associate', 'expert'];
    if (!validRoles.includes(role)) {
      return res.status(400).json({ error: 'Invalid role for signup' });
    }

    // Check if email exists
    const existingUser = await pool.query(
      'SELECT id FROM users WHERE email = $1',
      [email]
    );

    if (existingUser.rows.length > 0) {
      return res.status(400).json({ error: 'Email already registered' });
    }

    // Hash password
    const saltRounds = 10;
    const passwordHash = await bcrypt.hash(password, saltRounds);

    // Parse name into first_name and last_name if needed
    let firstName = first_name;
    let lastName = last_name;
    if (!firstName && !lastName && name) {
      const nameParts = name.trim().split(/\s+/);
      firstName = nameParts[0] || null;
      lastName = nameParts.slice(1).join(' ') || null;
    }

    // Create user (matches Mindaigle schema with first_name, last_name)
    const result = await pool.query(
      `INSERT INTO users (email, password_hash, first_name, last_name, role) 
       VALUES ($1, $2, $3, $4, $5) 
       RETURNING id, email, first_name, last_name, role, created_at`,
      [email, passwordHash, firstName, lastName, role]
    );

    const user = result.rows[0];

    // If parent, create pending link with student email
    if (role === 'parent' && studentEmail) {
      await pool.query(
        `INSERT INTO parent_child_links (parent_id, student_email, status) 
         VALUES ($1, $2, 'pending') 
         ON CONFLICT (parent_id, student_email) DO NOTHING`,
        [user.id, studentEmail]
      );
    }

    // Generate JWT token
    const token = jwt.sign(
      { userId: user.id, email: user.email, role: user.role },
      JWT_SECRET,
      { expiresIn: '7d' }
    );

    // Return user with name field for backward compatibility
    const userName = user.first_name && user.last_name 
      ? `${user.first_name} ${user.last_name}`.trim()
      : user.first_name || user.last_name || email.split('@')[0];

    res.status(201).json({
      success: true,
      userId: user.id,
      token,
      user: {
        id: user.id,
        email: user.email,
        name: userName,
        role: user.role
      }
    });
  } catch (error) {
    console.error('Signup error:', error);
    // Don't expose internal error details in production
    const errorMessage = process.env.NODE_ENV === 'production' 
      ? 'Server error' 
      : error.message;
    res.status(500).json({ 
      error: 'Server error',
      ...(process.env.NODE_ENV !== 'production' && { details: errorMessage })
    });
  }
});

// Max wait per DB for login (ms) – prevents slow login when a DB is unreachable or wrong port
const LOGIN_DB_TIMEOUT_MS = 4000;

function queryWithTimeout(p, query, params, ms) {
  return Promise.race([
    p.query(query, params),
    new Promise((_, reject) => setTimeout(() => reject(new Error('timeout')), ms))
  ]).catch(() => ({ rows: [] }));
}

// Login – query master and all school DBs in parallel with timeout so login stays fast
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }

    let user = null;
    let schoolId = null;
    let userPool = pool;
    let allPools = {};
    let schoolNames = [];
    try {
      allPools = getAllPools();
      schoolNames = Object.keys(allPools || {});
    } catch (e) {
      console.error('Login: getAllPools failed, using master DB only:', e.message);
    }

    const userQuery = 'SELECT id, email, password_hash, first_name, last_name, role FROM users WHERE email = $1';

    const promises = [
      queryWithTimeout(pool, userQuery, [email], LOGIN_DB_TIMEOUT_MS)
    ];
    schoolNames.forEach(name => {
      if (allPools[name]) {
        promises.push(
          queryWithTimeout(allPools[name], userQuery, [email], LOGIN_DB_TIMEOUT_MS)
        );
      }
    });

    const results = await Promise.all(promises);
    const masterResult = results[0];

    if (masterResult.rows && masterResult.rows.length > 0) {
      user = masterResult.rows[0];
      userPool = pool;
    } else {
      for (let i = 0; i < schoolNames.length; i++) {
        const r = results[i + 1];
        if (r && r.rows && r.rows.length > 0) {
          user = r.rows[0];
          userPool = allPools[schoolNames[i]];
          schoolId = 1;
          break;
        }
      }
    }

    if (user && userPool === pool) {
      // User found in master DB, get school_id for routing
      try {
        // Check if user is a student
        const studentResult = await pool.query(
          'SELECT school_id FROM students WHERE user_id = $1',
          [user.id]
        );
        
        if (studentResult.rows.length > 0) {
          schoolId = studentResult.rows[0].school_id;
        } else {
          // Check if user is an associate
          const associateResult = await pool.query(
            'SELECT school_id FROM associates WHERE user_id = $1 OR email = $2',
            [user.id, email]
          );
          
          if (associateResult.rows.length > 0) {
            schoolId = associateResult.rows[0].school_id;
          }
        }
      } catch (error) {
        console.error('Error getting school_id from master DB:', error.message);
      }
    }

    if (!user) {
      console.warn('Login failed: no user found for email:', email.replace(/.(?=.@)/g, '*'));
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    // Verify password
    const validPassword = await bcrypt.compare(password, user.password_hash);
    if (!validPassword) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    // Generate JWT token with school information
    // Since all databases have school_id = 1, we need to identify school by name
    const tokenPayload = {
      userId: user.id,
      email: user.email,
      role: user.role
    };
    
    // Determine school name based on which database user was found in
    let schoolName = null;
    if (userPool !== pool) {
      // User was found in a school database, determine which one
      const allPools = getAllPools();
      for (const [name, schoolPool] of Object.entries(allPools)) {
        if (schoolPool === userPool) {
          schoolName = name; // 'horizons', 'houghton', or 'calumet'
          break;
        }
      }
    } else {
      // User was found in master DB (e.g. single-DB mode with mindaigle)
      // Route by school_id when available (7→horizons, 8→houghton, 9→calumet), else default
      if (schoolId != null && getSchoolName(schoolId)) {
        schoolName = getSchoolName(schoolId);
      } else {
        schoolName = process.env.DEFAULT_SCHOOL_NAME || 'horizons';
      }
    }
    
    // Include school_id (will be 1) and schoolName for routing
    if (schoolId) {
      tokenPayload.schoolId = schoolId;
    }
    if (schoolName) {
      tokenPayload.schoolName = schoolName; // Use this for routing instead of schoolId
    }

    // Verify JWT_SECRET is set
    if (!JWT_SECRET) {
      console.error('ERROR: JWT_SECRET is not set!');
      return res.status(500).json({ error: 'Server configuration error' });
    }

    const token = jwt.sign(tokenPayload, JWT_SECRET, { expiresIn: '7d' });

    // Verify token was generated
    if (!token) {
      console.error('ERROR: Token generation failed!');
      return res.status(500).json({ error: 'Token generation failed' });
    }

    // Return user with name field for backward compatibility
    const userName = user.first_name && user.last_name 
      ? `${user.first_name} ${user.last_name}`.trim()
      : user.first_name || user.last_name || email.split('@')[0];

    res.json({
      success: true,
      token,
      user: {
        id: user.id,
        email: user.email,
        name: userName,
        role: user.role,
        schoolId: schoolId || undefined,
        schoolName: schoolName || undefined // Include schoolName for routing
      }
    });
  } catch (error) {
    console.error('Login error:', error);
    // Don't expose internal error details in production
    const errorMessage = process.env.NODE_ENV === 'production' 
      ? 'Server error' 
      : error.message;
    res.status(500).json({ 
      error: 'Server error',
      ...(process.env.NODE_ENV !== 'production' && { details: errorMessage })
    });
  }
});

// Get current user's routing info (which DB/school is used for data) - for verification
router.get('/routing-info', authenticateToken, (req, res) => {
  const schoolName = req.user.schoolName;
  const schoolId = req.user.schoolId;
  if (!schoolName) {
    return res.status(400).json({
      error: 'School not identified',
      hint: 'Login again; token may lack schoolName. Data requests will fail until schoolName is set.'
    });
  }
  res.json({
    schoolName,
    schoolId: schoolId ?? null,
    message: `Data is routed to school DB: ${schoolName}`
  });
});

// Get current user profile
// Uses authenticateToken middleware and routes to correct database based on schoolName
router.get('/profile', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const schoolName = req.user.schoolName;
    
    // Determine which database to query
    let userPool = pool; // Default to master database
    if (schoolName) {
      // User is in a school-specific database
      try {
        userPool = getPool(schoolName);
      } catch (error) {
        console.error(`Error getting pool for school ${schoolName}:`, error);
        // Fall back to master database
      }
    }
    
    // Query the appropriate database (include profile_picture_url if column exists)
    let result;
    try {
      result = await userPool.query(
        'SELECT id, email, first_name, last_name, role, created_at, profile_picture_url FROM users WHERE id = $1',
        [userId]
      );
    } catch (colErr) {
      if (colErr.code === '42703') {
        result = await userPool.query(
          'SELECT id, email, first_name, last_name, role, created_at FROM users WHERE id = $1',
          [userId]
        );
      } else throw colErr;
    }

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    const user = result.rows[0];
    
    // Return user with name field for backward compatibility
    const userName = user.first_name && user.last_name 
      ? `${user.first_name} ${user.last_name}`.trim()
      : user.first_name || user.last_name || user.email.split('@')[0];

    res.json({
      id: user.id,
      email: user.email,
      name: userName,
      role: user.role,
      created_at: user.created_at,
      profilePictureUrl: user.profile_picture_url || null,
      schoolId: req.user.schoolId || undefined,
      schoolName: schoolName || undefined
    });
  } catch (error) {
    console.error('Profile error:', error);
    res.status(401).json({ error: 'Invalid token' });
  }
});

// Update profile (name and/or profile picture URL)
router.put('/profile', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const schoolName = req.user.schoolName;
    const { name, profilePictureUrl } = req.body;
    let userPool = pool;
    if (schoolName) {
      try { userPool = getPool(schoolName); } catch (e) { /* use master */ }
    }
    const updates = [];
    const values = [];
    let i = 1;
    if (name != null && typeof name === 'string') {
      const parts = name.trim().split(/\s+/);
      updates.push(`first_name = $${i++}`, `last_name = $${i++}`);
      values.push(parts[0] || null, parts.slice(1).join(' ') || null);
    }
    if (profilePictureUrl != null && typeof profilePictureUrl === 'string') {
      updates.push(`profile_picture_url = $${i++}`);
      values.push(profilePictureUrl.trim() || null);
    }
    if (updates.length === 0) return res.status(400).json({ error: 'Nothing to update' });
    values.push(userId);
    const result = await userPool.query(
      `UPDATE users SET ${updates.join(', ')} WHERE id = $${i} RETURNING id, first_name, last_name, profile_picture_url`,
      values
    );
    if (result.rows.length === 0) return res.status(404).json({ error: 'User not found' });
    const row = result.rows[0];
    res.json({
      profilePictureUrl: row.profile_picture_url || null,
      name: [row.first_name, row.last_name].filter(Boolean).join(' ') || null
    });
  } catch (error) {
    console.error('Update profile error:', error);
    res.status(500).json({ error: 'Failed to update profile' });
  }
});

// Upload profile photo (base64). Saves to public/images/users/{userId}.jpg and sets profile_picture_url.
router.post('/profile/photo', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const schoolName = req.user.schoolName;
    const { image: base64Image } = req.body; // base64 data URL or raw base64
    if (!base64Image || typeof base64Image !== 'string') {
      return res.status(400).json({ error: 'Missing image (base64)' });
    }
    const basePath = path.join(__dirname, '..', 'public', 'images', 'users');
    if (!fs.existsSync(basePath)) {
      fs.mkdirSync(basePath, { recursive: true });
    }
    let buffer;
    const base64Data = base64Image.replace(/^data:image\/\w+;base64,/, '');
    buffer = Buffer.from(base64Data, 'base64');
    if (buffer.length > 5 * 1024 * 1024) return res.status(400).json({ error: 'Image too large (max 5MB)' });
    const ext = base64Image.includes('image/png') ? 'png' : 'jpg';
    const filename = `${userId}.${ext}`;
    const filePath = path.join(basePath, filename);
    fs.writeFileSync(filePath, buffer);
    const profilePictureUrl = `/images/users/${filename}`;
    let userPool = pool;
    if (schoolName) {
      try { userPool = getPool(schoolName); } catch (e) { /* use master */ }
    }
    await userPool.query(
      'UPDATE users SET profile_picture_url = $1 WHERE id = $2',
      [profilePictureUrl, userId]
    );
    res.json({ profilePictureUrl });
  } catch (error) {
    console.error('Profile photo upload error:', error);
    res.status(500).json({ error: 'Failed to upload photo' });
  }
});

module.exports = router;

