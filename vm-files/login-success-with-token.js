/**
 * Copy this file to the VM backend and use it in your login route.
 * Usage: sendLoginSuccess(res, user)
 * Requires: npm install jsonwebtoken (on the VM backend).
 */
const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-key-change-in-production';

/**
 * Sends the login success response with a JWT token (required by the Android app).
 * @param {object} res - Express response object
 * @param {object} user - User row from DB (must have id, email, role; optional first_name, last_name)
 */
function sendLoginSuccess(res, user) {
  const userName = (user.first_name && user.last_name)
    ? `${user.first_name} ${user.last_name}`.trim()
    : user.first_name || user.last_name || (user.email && user.email.split('@')[0]) || 'User';

  const token = jwt.sign(
    { userId: user.id, email: user.email, role: user.role },
    JWT_SECRET,
    { expiresIn: '7d' }
  );

  res.json({
    success: true,
    token,
    user: {
      id: user.id,
      email: user.email,
      name: userName,
      role: user.role
    }
  });
}

module.exports = { sendLoginSuccess };
