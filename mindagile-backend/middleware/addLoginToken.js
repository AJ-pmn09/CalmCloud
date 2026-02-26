/**
 * Ensures POST /api/auth/login responses include a JWT token.
 * If the auth route returns { success: true, data: { user } } without a token,
 * this middleware rewrites the response to { success: true, token, user }.
 * Use this so the Android app always receives a token regardless of auth route version.
 */
const jwt = require('jsonwebtoken');
const { JWT_SECRET } = require('./auth');

module.exports = function addLoginTokenMiddleware(req, res, next) {
  const isLogin = req.path === '/login' || req.path === '/auth/login';
  if (req.method !== 'POST' || !isLogin) return next();

  const originalJson = res.json.bind(res);
  res.json = function (body) {
    if (body && body.success === true && !body.token) {
      const user = (body.data && body.data.user) ? body.data.user : body.user;
      if (user && user.id != null && user.email) {
        const secret = JWT_SECRET || process.env.JWT_SECRET || 'dev-secret-key-change-in-production';
        const token = jwt.sign(
          { userId: user.id, email: user.email, role: user.role },
          secret,
          { expiresIn: '7d' }
        );
        const name = (user.first_name && user.last_name)
          ? `${user.first_name} ${user.last_name}`.trim()
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
