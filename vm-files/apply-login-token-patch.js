#!/usr/bin/env node
/**
 * Run this ON THE VM from the backend folder (or with BACKEND_DIR set)
 * to add the JWT login response to the auth route.
 * Usage: node apply-login-token-patch.js
 *        node apply-login-token-patch.js --dump   (print auth file for debugging)
 */
const fs = require('fs');
const path = require('path');

const DUMP = process.argv.includes('--dump');
const BACKEND_DIR = process.env.BACKEND_DIR || __dirname;
const authPaths = [
  path.join(BACKEND_DIR, 'routes', 'auth.js'),
  path.join(BACKEND_DIR, 'src', 'routes', 'auth.js'),
].filter(p => fs.existsSync(p));

if (authPaths.length === 0) {
  console.error('Could not find auth.js in backend/routes/ or backend/src/routes/. Set BACKEND_DIR if needed.');
  process.exit(1);
}

if (DUMP) {
  authPaths.forEach(authPath => {
    const lines = fs.readFileSync(authPath, 'utf8').split('\n');
    console.log('--- ' + authPath + ' ---\n');
    lines.forEach((line, i) => {
      if (/res\.json|success|data|user|Login/i.test(line)) console.log((i + 1) + ':', line);
    });
    console.log('');
  });
  process.exit(0);
}

// Patch every auth.js that exists (server might load from routes/ or src/routes/)
let anyPatched = false;
for (const authPath of authPaths) {
  const authDir = path.dirname(authPath);
  const relToBackend = path.relative(BACKEND_DIR, authDir);
  const requirePath = relToBackend.split(path.sep).length >= 2 ? '../../login-success-with-token' : '../login-success-with-token';

  let content = fs.readFileSync(authPath, 'utf8');

  // 1. Add require if not present
  const requireLine = `const { sendLoginSuccess } = require('${requirePath}');`;
  if (!content.includes('sendLoginSuccess')) {
    const lastRequire = content.match(/require\([^)]+\);\s*\n/g);
    const insertAt = lastRequire ? content.lastIndexOf(lastRequire[lastRequire.length - 1]) + lastRequire[lastRequire.length - 1].length : 0;
    content = content.slice(0, insertAt) + '\n' + requireLine + '\n' + content.slice(insertAt);
  }

  // 2. Replace common login success responses with sendLoginSuccess(res, user)
  const patterns = [
    /res\.json\s*\(\s*\{\s*success\s*:\s*true\s*,\s*message\s*:\s*["']Login successful["']\s*,\s*data\s*:\s*\{\s*user\s*\}\s*\}\s*\)\s*;?/g,
    /res\.json\s*\(\s*\{\s*success\s*:\s*true\s*,\s*data\s*:\s*\{\s*user\s*\}\s*\}\s*\)\s*;?/g,
    /return\s+res\.json\s*\(\s*\{\s*success\s*:\s*true\s*,\s*message\s*:\s*["']Login successful["']\s*,\s*data\s*:\s*\{\s*user\s*\}\s*\}\s*\)\s*;?/g,
    /return\s+res\.json\s*\(\s*\{\s*success\s*:\s*true\s*,\s*data\s*:\s*\{\s*user\s*\}\s*\}\s*\)\s*;?/g,
    /res\.json\s*\(\s*\{\s*success\s*:\s*true\s*,\s*message\s*:\s*["'][^"']*["']\s*,\s*data\s*:\s*\{\s*user\s*:\s*[^}]+}\s*\}\s*\)\s*;?/g,
    /res\.status\s*\(\s*200\s*\)\s*\.json\s*\(\s*\{\s*success\s*:\s*true\s*,[\s\S]*?data\s*:\s*\{\s*user[\s\S]*?\}\s*\)\s*;?/g,
    /res\.json\s*\(\s*\{\s*success\s*:\s*true\s*,[\s\S]*?data\s*:\s*\{\s*user[\s\S]*?\}\s*\}\s*\)\s*;?/g,
  ];

  let replaced = false;
  for (let i = 0; i < patterns.length; i++) {
    const re = patterns[i];
    const replacement = re.source.includes('return') ? 'return sendLoginSuccess(res, user);' : 'sendLoginSuccess(res, user);';
    const newContent = content.replace(re, replacement);
    if (newContent !== content) {
      content = newContent;
      replaced = true;
      break;
    }
  }
  if (!replaced) {
    const multiline = /res\.json\s*\(\s*\{[\s\S]*?success\s*:\s*true[\s\S]*?data\s*:\s*\{[\s\S]*?user[\s\S]*?\}[\s\S]*?\}\s*\)\s*;?/;
    const before = content;
    content = content.replace(multiline, 'sendLoginSuccess(res, user);');
    replaced = content !== before;
  }
  if (!replaced) {
    const withMessage = /res\.json\s*\(\s*\{[\s\S]*?Login successful[\s\S]*?data[\s\S]*?\}\s*\)\s*;?/;
    const before = content;
    content = content.replace(withMessage, 'sendLoginSuccess(res, user);');
    replaced = content !== before;
  }
  if (!replaced) {
    const newlines = /res\.json\s*\(\s*\{\s*success\s*:\s*true\s*,\s*message\s*:\s*["']Login\s+successful["']\s*,\s*data\s*:\s*\{\s*user\s*\}\s*\}\s*\)\s*;?/;
    const before = content;
    content = content.replace(newlines, 'sendLoginSuccess(res, user);');
    replaced = content !== before;
  }
  if (!replaced) {
    const newlines2 = /res\.json\s*\(\s*\{\s*success\s*:\s*true\s*,\s*data\s*:\s*\{\s*user\s*\}\s*\}\s*\)\s*;?/;
    const before = content;
    content = content.replace(newlines2, 'sendLoginSuccess(res, user);');
    replaced = content !== before;
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
  console.log('Patched:', authPath);
  anyPatched = true;
}

if (!anyPatched) {
  console.error('No auth file was patched. Check that login success response exists.');
  process.exit(1);
}
console.log('Restart the backend (e.g. ./STOP_ALL.sh then ./START_ALL.sh).');
