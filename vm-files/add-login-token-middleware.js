/**
 * Drop-in middleware: adds a JWT token to login responses that have success + user but no token.
 * Copy to VM backend and in your main server file (index.js or server.js) add:
 *
 *   const addLoginToken = require('./add-login-token-middleware');
 *   app.use('/api/auth', addLoginToken);   // BEFORE app.use('/api/auth', authRoutes)
 *   app.use('/api/auth', authRoutes);
 *
 * Requires: npm install jsonwebtoken
 */
const jwt = require('jsonwebtoken');
const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-key-change-in-production';

module.exports = function addLoginTokenMiddleware(req, res, next) {
  const isLogin = req.path === '/login' || req.path === '/auth/login';
  if (req.method !== 'POST' || !isLogin) return next();

  const originalJson = res.json.bind(res);
  res.json = function (body) {
    if (body && body.success === true && !body.token) {
      const user = (body.data && body.data.user) ? body.data.user : body.user;
      if (user && user.id != null && user.email) {
        const token = jwt.sign(
          { userId: user.id, email: user.email, role: user.role },
          JWT_SECRET,
          { expiresIn: '7d' }
        );
        const name = (user.first_name && user.last_name)
          ? (user.first_name + ' ' + user.last_name).trim()
          : user.first_name || user.last_name || (user.email && user.email.split('@')[0]) || 'User';
        return originalJson({
          success: true,
          token,
          user: { id: user.id, email: user.email, name, role: user.role }
        });
      }
    }
    return originalJson(body);
  };
  next();
};
