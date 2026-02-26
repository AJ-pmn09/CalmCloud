#!/usr/bin/env node
/**
 * Run ON THE VM from the backend folder. Finds the main server file, ensures
 * routes/studentData.js exists, and adds require + app.use('/api', studentDataRoutes).
 *
 * Prerequisite: copy studentData.js to the backend first, e.g.:
 *   From Mac: scp mindagile-backend/routes/studentData.js vkasarla@<VM_IP>:/home/vkasarla/Desktop/K-12/backend/src/routes/
 *   or to backend/routes/ if your main file is index.js in backend root.
 *
 *   cd /home/vkasarla/Desktop/K-12/backend
 *   node add-student-data-route.js
 *
 * Then restart: cd .. && ./STOP_ALL.sh && ./START_ALL.sh
 */
const fs = require('fs');
const path = require('path');

const BACKEND_DIR = process.env.BACKEND_DIR || path.resolve(__dirname);

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

let patchedAny = false;
for (const mainPath of mainPaths) {
  const mainDir = path.dirname(mainPath);
  const routesInMainDir = path.join(mainDir, 'routes', 'studentData.js');
  const routesInBackend = path.join(BACKEND_DIR, 'routes', 'studentData.js');

  // Prefer backend/routes/ when main file is in src/ so studentData.js can require('../middleware/') from backend root
  let requirePath;
  if (mainDir !== BACKEND_DIR && fs.existsSync(routesInBackend)) {
    const rel = path.relative(mainDir, path.join(BACKEND_DIR, 'routes', 'studentData'));
    requirePath = rel.replace(/\\/g, '/');
  } else if (fs.existsSync(routesInMainDir)) {
    requirePath = './routes/studentData';
  } else if (fs.existsSync(routesInBackend)) {
    requirePath = './routes/studentData';
  } else {
    console.error('Missing routes/studentData.js. Copy it to either:');
    console.error('  ', routesInMainDir);
    console.error('  ', routesInBackend);
    console.error('From Mac: scp mindagile-backend/routes/studentData.js vkasarla@<VM>:' + path.dirname(routesInMainDir) + '/');
    process.exit(1);
  }

  let content = fs.readFileSync(mainPath, 'utf8');

  const hasRequire = /const\s+studentDataRoutes\s*=\s*require\s*\(/.test(content);
  const hasUse = /app\.use\s*\(\s*['"]\/api['"]\s*,\s*studentDataRoutes\s*\)/.test(content);

  // If app.use was added but require is missing, add the require
  if (!hasRequire && hasUse) {
    const requireLine = `const studentDataRoutes = require('${requirePath}');`;
    const lastRequire = content.match(/require\([^)]+\);\s*\n/g);
    const insertAt = lastRequire
      ? content.lastIndexOf(lastRequire[lastRequire.length - 1]) + lastRequire[lastRequire.length - 1].length
      : 0;
    content = content.slice(0, insertAt) + '\n' + requireLine + '\n' + content.slice(insertAt);
    fs.writeFileSync(mainPath, content, 'utf8');
    console.log('Added missing studentDataRoutes require in', mainPath);
    patchedAny = true;
    continue;
  }

  // If main file is in src/ but currently requires ./routes/studentData, fix to ../routes/studentData so backend/routes and backend/middleware are used
  if (hasRequire && mainDir !== BACKEND_DIR && fs.existsSync(routesInBackend)) {
    const wrongRequireRe = /const\s+studentDataRoutes\s*=\s*require\s*\(\s*['"]\.\/routes\/studentData['"]\s*\)/;
    if (wrongRequireRe.test(content)) {
      content = content.replace(wrongRequireRe, `const studentDataRoutes = require('${requirePath}');`);
      fs.writeFileSync(mainPath, content, 'utf8');
      console.log('Fixed studentData require path to use backend/routes in', mainPath);
      patchedAny = true;
      continue;
    }
  }

  if (hasRequire && hasUse) {
    console.log('Student-data route already added in', mainPath);
    patchedAny = true;
    continue;
  }

  if (!hasRequire) {
    const requireLine = `const studentDataRoutes = require('${requirePath}');`;
    const lastRequire = content.match(/require\([^)]+\);\s*\n/g);
    const insertAt = lastRequire
      ? content.lastIndexOf(lastRequire[lastRequire.length - 1]) + lastRequire[lastRequire.length - 1].length
      : 0;
    content = content.slice(0, insertAt) + '\n' + requireLine + '\n' + content.slice(insertAt);
  }

  if (!hasUse) {
    // Insert after auth routes (last line that mounts /api/auth)
    const authLineRe = /app\.use\s*\(\s*['"]\/api\/auth['"][^)]*\)\s*;?\s*\n/g;
    let lastMatch;
    let m;
    while ((m = authLineRe.exec(content)) !== null) lastMatch = m;
    if (lastMatch) {
      const insertAfter = lastMatch.index + lastMatch[0].length;
      content = content.slice(0, insertAfter) + "app.use('/api', studentDataRoutes);\n" + content.slice(insertAfter);
    } else {
      const apiUseRe = /app\.use\s*\(\s*['"]\/api['"][^)]*\)\s*;?\s*\n/;
      const m2 = content.match(apiUseRe);
      if (m2) {
        const insertAfter = content.indexOf(m2[0]) + m2[0].length;
        content = content.slice(0, insertAfter) + "app.use('/api', studentDataRoutes);\n" + content.slice(insertAfter);
      } else {
        console.error('Could not find a good place to add app.use in', mainPath);
        continue;
      }
    }
  }

  fs.writeFileSync(mainPath, content, 'utf8');
  console.log('Patched', mainPath, '- added studentData route');
  patchedAny = true;
}

if (!patchedAny) process.exit(1);
console.log('Restart backend: cd /home/vkasarla/Desktop/K-12 && ./STOP_ALL.sh && ./START_ALL.sh');
