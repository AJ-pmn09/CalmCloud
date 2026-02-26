/**
 * COPY THIS into your VM backend login route, after you have verified the password.
 * Replace your current "success" response (e.g. res.json({ success: true, data: { user } }))
 * with: first require jwt at top of file, then the two blocks below.
 *
 * At top of your auth file:
 *   const jwt = require('jsonwebtoken');
 *
 * Where you send login success (after password is valid), use this:
 */

// 1) Secret (put with your other config, or in .env as JWT_SECRET)
const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-key-change-in-production';

// 2) After you have the `user` row from the DB, build name and token:
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
