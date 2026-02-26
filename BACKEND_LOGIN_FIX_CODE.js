// Fix for routes/auth.js login route
// Replace the res.json() call in the login route with this:

// After generating token (around line 230-240)
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

// FIXED RESPONSE - Include token at top level
res.json({
  success: true,
  token: token,  // ← CRITICAL: Token must be here
  user: {
    id: user.id,
    email: user.email,
    name: userName,
    role: user.role
  },
  schoolName: schoolName  // Optional - include if available
});

// OLD (WRONG) FORMAT - DO NOT USE:
// res.json({
//   success: true,
//   message: "Login successful",
//   data: {
//     user: {...}
//   }
// });
