#!/usr/bin/env node
/**
 * Run ON THE VM from the backend folder. Finds the main server file (index.js or src/server.js),
 * ensures add-login-token-middleware.js is there, and adds the two lines before auth routes.
 *
 *   cd /home/vkasarla/Desktop/K-12/backend
 *   node add-middleware-to-server.js
 *
 * Then restart: cd .. && ./STOP_ALL.sh && ./START_ALL.sh
 */
const fs = require('fs');
const path = require('path');

const BACKEND_DIR = process.env.BACKEND_DIR || __dirname;

// Find ALL server files that mount /api/auth (backend might be started from index.js OR src/server.js)
function findMainFiles() {
  const candidates = [
    path.join(BACKEND_DIR, 'index.js'),
    path.join(BACKEND_DIR, 'src', 'server.js'),
    path.join(BACKEND_DIR, 'src', 'index.js'),
    path.join(BACKEND_DIR, 'app.js'),
  ].filter(p => fs.existsSync(p));

  return candidates.filter(p => {
    const content = fs.readFileSync(p, 'utf8');
    return /app\.use\s*\(\s*['"]\/api\/auth['"]/.test(content);
  });
}

const mainPaths = findMainFiles();
if (mainPaths.length === 0) {
  console.error('Could not find any server file with app.use("/api/auth"). Set BACKEND_DIR if needed.');
  process.exit(1);
}

const middlewareName = 'add-login-token-middleware.js';
const middlewareInBackend = path.join(BACKEND_DIR, middlewareName);
if (!fs.existsSync(middlewareInBackend)) {
  console.error('Missing', middlewareName, 'in', BACKEND_DIR);
  console.error('Copy from Mac: scp vm-files/add-login-token-middleware.js vkasarla@<VM>:' + BACKEND_DIR + '/');
  process.exit(1);
}

let patchedAny = false;
for (const mainPath of mainPaths) {
  const mainDir = path.dirname(mainPath);
  const middlewareInSameDir = path.join(mainDir, middlewareName);

  if (!fs.existsSync(middlewareInSameDir)) {
    fs.copyFileSync(middlewareInBackend, middlewareInSameDir);
    console.log('Copied', middlewareName, 'to', mainDir);
  }

  let content = fs.readFileSync(mainPath, 'utf8');
  const authMountRe = /(\s*)app\.use\s*\(\s*['"]\/api\/auth['"]\s*,\s*(\w+)\s*\)\s*;?/;

  // If addLoginToken is present but appears AFTER authRoutes, reorder so addLoginToken comes first
  let reordered = false;
  const addLoginTokenLineRe = /\s*app\.use\s*\(\s*['"]\/api\/auth['"]\s*,\s*addLoginToken\s*\)\s*;?\s*\n?/g;
  const authMatch = content.match(authMountRe);
  const addLoginTokenIdx = content.indexOf("app.use('/api/auth', addLoginToken)");
  if (authMatch && addLoginTokenIdx >= 0) {
    const authRoutesIdx = content.indexOf(authMatch[0]);
    if (authRoutesIdx >= 0 && addLoginTokenIdx > authRoutesIdx) {
      content = content.replace(addLoginTokenLineRe, '');
      const match2 = content.match(authMountRe);
      if (match2) {
        content = content.replace(authMountRe, match2[1] + "app.use('/api/auth', addLoginToken);\n" + match2[0]);
        reordered = true;
      }
    }
  }

  const requireLine = "const addLoginToken = require('./add-login-token-middleware');";
  const hasRequire = content.includes('add-login-token-middleware') && content.includes('addLoginToken');
  const hasAddLoginTokenUse = /app\.use\s*\(\s*['"]\/api\/auth['"]\s*,\s*addLoginToken\s*\)/.test(content);

  if (hasRequire && hasAddLoginTokenUse && !reordered) {
    console.log('Middleware already correctly added in', mainPath);
    patchedAny = true;
    continue;
  }

  if (!content.includes('addLoginToken')) {
    const lastRequire = content.match(/require\([^)]+\);\s*\n/g);
    const insertAt = lastRequire
      ? content.lastIndexOf(lastRequire[lastRequire.length - 1]) + lastRequire[lastRequire.length - 1].length
      : 0;
    content = content.slice(0, insertAt) + '\n' + requireLine + '\n' + content.slice(insertAt);
  }

  if (!content.includes("app.use('/api/auth', addLoginToken)")) {
    const match = content.match(authMountRe);
    if (match) {
      content = content.replace(authMountRe, match[1] + "app.use('/api/auth', addLoginToken);\n" + match[0]);
    }
  }

  fs.writeFileSync(mainPath, content, 'utf8');
  console.log('Patched', mainPath);
  patchedAny = true;
}

if (!patchedAny) process.exit(1);
console.log('Restart backend: cd /home/vkasarla/Desktop/K-12 && ./STOP_ALL.sh && ./START_ALL.sh');
