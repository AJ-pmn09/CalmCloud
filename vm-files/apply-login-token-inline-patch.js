#!/usr/bin/env node
/**
 * Patches auth.js by INLINING JWT logic (no external login-success-with-token.js).
 * Run on VM from backend folder: node apply-login-token-inline-patch.js
 * Requires: npm install jsonwebtoken (in backend).
 */
const fs = require('fs');
const path = require('path');

const BACKEND_DIR = process.env.BACKEND_DIR || __dirname;
const authPaths = [
  path.join(BACKEND_DIR, 'routes', 'auth.js'),
  path.join(BACKEND_DIR, 'src', 'routes', 'auth.js'),
].filter(p => fs.existsSync(p));

if (authPaths.length === 0) {
  console.error('Could not find auth.js in backend/routes/ or backend/src/routes/. Set BACKEND_DIR if needed.');
  process.exit(1);
}

const INLINE_BLOCK = `
const jwt = require('jsonwebtoken');
const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-key-change-in-production';
function sendLoginSuccess(res, user) {
  const userName = (user.first_name && user.last_name) ? (user.first_name + ' ' + user.last_name).trim() : user.first_name || user.last_name || (user.email && user.email.split('@')[0]) || 'User';
  const token = jwt.sign({ userId: user.id, email: user.email, role: user.role }, JWT_SECRET, { expiresIn: '7d' });
  res.json({ success: true, token, user: { id: user.id, email: user.email, name: userName, role: user.role } });
}
`;

let anyPatched = false;
for (const authPath of authPaths) {
  let content = fs.readFileSync(authPath, 'utf8');

  if (content.includes('sendLoginSuccess(res, user)') && content.includes('jwt.sign')) {
    console.log('Already patched (inline):', authPath);
    anyPatched = true;
    continue;
  }

  // 1. Add inline JWT + sendLoginSuccess if not present
  if (!content.includes('sendLoginSuccess') || !content.includes('jwt.sign')) {
    const lastRequire = content.match(/require\([^)]+\);\s*\n/g);
    const insertAt = lastRequire ? content.lastIndexOf(lastRequire[lastRequire.length - 1]) + lastRequire[lastRequire.length - 1].length : 0;
    content = content.slice(0, insertAt) + INLINE_BLOCK + content.slice(insertAt);
  }

  // 2. Replace login success response with sendLoginSuccess(res, user)
  const patterns = [
    /res\.json\s*\(\s*\{\s*success\s*:\s*true\s*,\s*message\s*:\s*["']Login successful["']\s*,\s*data\s*:\s*\{\s*user\s*\}\s*\}\s*\)\s*;?/g,
    /res\.json\s*\(\s*\{\s*success\s*:\s*true\s*,\s*data\s*:\s*\{\s*user\s*\}\s*\}\s*\)\s*;?/g,
    /return\s+res\.json\s*\(\s*\{\s*success\s*:\s*true\s*,\s*message\s*:\s*["']Login successful["']\s*,\s*data\s*:\s*\{\s*user\s*\}\s*\}\s*\)\s*;?/g,
    /return\s+res\.json\s*\(\s*\{\s*success\s*:\s*true\s*,\s*data\s*:\s*\{\s*user\s*\}\s*\}\s*\)\s*;?/g,
    /res\.json\s*\(\s*\{\s*success\s*:\s*true\s*,[\s\S]*?data\s*:\s*\{\s*user[\s\S]*?\}\s*\}\s*\)\s*;?/g,
  ];

  let replaced = false;
  for (const re of patterns) {
    const replacement = re.source.includes('return') ? 'return sendLoginSuccess(res, user);' : 'sendLoginSuccess(res, user);';
    const newContent = content.replace(re, replacement);
    if (newContent !== content) {
      content = newContent;
      replaced = true;
      break;
    }
  }
  if (!replaced) {
    const re = /(return\s+)?res\.json\s*\(\s*\{/g;
    let match;
    while ((match = re.exec(content)) !== null) {
      const start = match.index;
      const braceStart = content.indexOf('{', start);
      let depth = 1;
      let i = braceStart + 1;
      while (i < content.length && depth > 0) {
        const c = content[i];
        if (c === '{') depth++;
        else if (c === '}') depth--;
        i++;
      }
      const end = content.indexOf(');', i - 1);
      if (end === -1) continue;
      const block = content.slice(start, end + 2);
      const looksLikeLogin = block.includes('success') && (block.includes('user') || block.includes('data')) &&
          (block.includes('Login successful') || (block.includes('message') && block.includes('data')));
      if (looksLikeLogin) {
        const replacement = (match[1] ? 'return ' : '') + 'sendLoginSuccess(res, user);';
        content = content.slice(0, start) + replacement + content.slice(end + 2);
        replaced = true;
        break;
      }
    }
  }

  if (!replaced) {
    console.error('Could not find login success res.json in:', authPath);
    continue;
  }

  fs.writeFileSync(authPath, content, 'utf8');
  console.log('Patched (inline JWT):', authPath);
  anyPatched = true;
}

if (!anyPatched) {
  console.error('No auth file was patched.');
  process.exit(1);
}
console.log('Restart backend (./STOP_ALL.sh then ./START_ALL.sh), then test login.');
