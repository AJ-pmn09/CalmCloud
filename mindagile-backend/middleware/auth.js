const jwt = require('jsonwebtoken');

// Require JWT_SECRET to be set in production
const JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET && process.env.NODE_ENV === 'production') {
  console.error('ERROR: JWT_SECRET must be set in production environment!');
  process.exit(1);
}
// Use a default only for development
const DEFAULT_SECRET = 'dev-secret-key-change-in-production';
const SECRET = JWT_SECRET || DEFAULT_SECRET;

if (!JWT_SECRET && process.env.NODE_ENV !== 'production') {
  console.warn('⚠️  WARNING: Using default JWT_SECRET. Set JWT_SECRET in .env for production!');
}

const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1]; // Bearer TOKEN

  if (!token) {
    return res.status(401).json({ error: 'Access token required' });
  }

  jwt.verify(token, SECRET, (err, user) => {
    if (err) {
      return res.status(403).json({ error: 'Invalid or expired token' });
    }
    req.user = user;
    next();
  });
};

const requireRole = (...roles) => {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Authentication required' });
    }
    if (!roles.includes(req.user.role)) {
      return res.status(403).json({ error: 'Insufficient permissions' });
    }
    next();
  };
};

module.exports = { authenticateToken, requireRole, JWT_SECRET: SECRET };

