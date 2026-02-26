const express = require('express');
const { authenticateToken, requireRole } = require('../middleware/auth');
const { routeToSchoolDatabase } = require('../middleware/databaseRouter');
const { getPool } = require('../config/databaseManager');
const {
  PHQ9_QUESTIONS,
  GAD7_QUESTIONS,
  scorePHQ9,
  scoreGAD7
} = require('../services/screenerScoring');

const router = express.Router();

const ANSWER_SCALE = [0, 1, 2, 3];

/** Create screener_instances, screener_responses, screener_scores, notifications if missing (per-school DB). */
async function ensureScreenerTables(pool) {
  try {
    await pool.query(`
      CREATE TABLE IF NOT EXISTS screener_instances (
        id SERIAL PRIMARY KEY,
        student_id INTEGER NOT NULL REFERENCES students(id) ON DELETE CASCADE,
        screener_type VARCHAR(50) NOT NULL,
        status VARCHAR(50) DEFAULT 'assigned',
        trigger_source VARCHAR(50) DEFAULT 'manual',
        assigned_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
        completed_at TIMESTAMP,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);
    await pool.query(`
      CREATE TABLE IF NOT EXISTS screener_responses (
        id SERIAL PRIMARY KEY,
        instance_id INTEGER NOT NULL REFERENCES screener_instances(id) ON DELETE CASCADE,
        question_index INTEGER NOT NULL,
        answer_value INTEGER NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE(instance_id, question_index)
      )
    `);
    await pool.query(`
      CREATE TABLE IF NOT EXISTS screener_scores (
        id SERIAL PRIMARY KEY,
        instance_id INTEGER NOT NULL UNIQUE REFERENCES screener_instances(id) ON DELETE CASCADE,
        total INTEGER NOT NULL,
        severity VARCHAR(50) NOT NULL,
        positive BOOLEAN NOT NULL DEFAULT false,
        details JSONB,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);
    await pool.query(`
      CREATE TABLE IF NOT EXISTS notifications (
        id SERIAL PRIMARY KEY,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        title VARCHAR(255) NOT NULL,
        body TEXT,
        related_type VARCHAR(100),
        related_id INTEGER,
        read_at TIMESTAMP,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);
  } catch (err) {
    console.error('ensureScreenerTables error:', err.message);
  }
  await ensureScreenerSchemaFix(pool);
}

/** Fix schema if VM/DB has different column names (e.g. screener_instance_id -> instance_id, score -> total). */
async function ensureScreenerSchemaFix(pool) {
  try {
    for (const table of ['screener_responses', 'screener_scores']) {
      const col = await pool.query(
        `SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = $1 AND column_name IN ('instance_id', 'screener_instance_id')`,
        [table]
      );
      const hasInstanceId = col.rows.some(r => r.column_name === 'instance_id');
      const hasScreenerInstanceId = col.rows.some(r => r.column_name === 'screener_instance_id');
      if (hasInstanceId) { /* ok */ } else if (hasScreenerInstanceId) {
        await pool.query(`ALTER TABLE ${table} RENAME COLUMN screener_instance_id TO instance_id`);
      } else {
        await pool.query(
          `ALTER TABLE ${table} ADD COLUMN instance_id INTEGER REFERENCES screener_instances(id) ON DELETE CASCADE`
        );
      }
    }

    // screener_scores: ensure total, severity, positive, details exist (VM may use score vs total etc.)
    const scoresCols = await pool.query(
      `SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'screener_scores'`
    );
    const has = (name) => scoresCols.rows.some(r => r.column_name === name);
    if (!has('total')) {
      if (has('score')) await pool.query(`ALTER TABLE screener_scores RENAME COLUMN score TO total`);
      else await pool.query(`ALTER TABLE screener_scores ADD COLUMN total INTEGER NOT NULL DEFAULT 0`);
    }
    if (!has('severity')) await pool.query(`ALTER TABLE screener_scores ADD COLUMN severity VARCHAR(50) NOT NULL DEFAULT 'unknown'`);
    if (!has('positive')) await pool.query(`ALTER TABLE screener_scores ADD COLUMN positive BOOLEAN NOT NULL DEFAULT false`);
    if (!has('details')) await pool.query(`ALTER TABLE screener_scores ADD COLUMN details JSONB`);

    // notifications: ensure body, title, related_type, related_id, read_at exist (VM may use message vs body)
    const notifCols = await pool.query(
      `SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'notifications'`
    );
    const notifHas = (name) => notifCols.rows.some(r => r.column_name === name);
    if (!notifHas('body')) {
      if (notifHas('message')) await pool.query(`ALTER TABLE notifications RENAME COLUMN message TO body`);
      else await pool.query(`ALTER TABLE notifications ADD COLUMN body TEXT`);
    }
    if (!notifHas('title')) await pool.query(`ALTER TABLE notifications ADD COLUMN title VARCHAR(255) NOT NULL DEFAULT ''`);
    if (!notifHas('related_type')) await pool.query(`ALTER TABLE notifications ADD COLUMN related_type VARCHAR(100)`);
    if (!notifHas('related_id')) await pool.query(`ALTER TABLE notifications ADD COLUMN related_id INTEGER`);
    if (!notifHas('read_at')) await pool.query(`ALTER TABLE notifications ADD COLUMN read_at TIMESTAMP`);
  } catch (err) {
    if (err.code !== '42701' && err.code !== '42P07') console.error('ensureScreenerSchemaFix error:', err.message);
  }
}

// --- Catalog (always works; no DB required)
router.get('/screeners/catalog', authenticateToken, (req, res) => {
  res.json({
    success: true,
    data: [
      { screener_type: 'phq9', name: 'PHQ-9', questions: PHQ9_QUESTIONS, answer_scale: ANSWER_SCALE },
      { screener_type: 'gad7', name: 'GAD-7', questions: GAD7_QUESTIONS, answer_scale: ANSWER_SCALE }
    ]
  });
});

// --- Create instance (staff/associate/admin assigns; student can self-start for own account)
router.post('/screeners/instances', authenticateToken, requireRole('associate', 'staff', 'admin', 'expert', 'student'), routeToSchoolDatabase, async (req, res) => {
  try {
    const { student_id: bodyStudentId, screener_type: type, trigger_source: triggerSource, override_frequency: overrideFrequency } = req.body;
    const schoolPool = req.db || (req.user?.schoolName ? getPool(req.user.schoolName) : null);
    if (!schoolPool) {
      return res.status(500).json({ success: false, error: 'Database not available.' });
    }
    await ensureScreenerTables(schoolPool);
    if (!type) {
      return res.status(400).json({ success: false, error: 'screener_type is required (phq9 or gad7).' });
    }
    const normalizedType = String(type).toLowerCase();
    if (!['phq9', 'gad7'].includes(normalizedType)) {
      return res.status(400).json({ success: false, error: 'screener_type must be phq9 or gad7.' });
    }

    let studentId;
    let assignedBy = null;
    if (req.user.role === 'student') {
      const me = await schoolPool.query('SELECT id FROM students WHERE user_id = $1', [req.user.userId]);
      if (me.rows.length === 0) return res.status(404).json({ success: false, error: 'Student profile not found.' });
      studentId = me.rows[0].id;
      if (bodyStudentId != null && Number(bodyStudentId) !== studentId) {
        return res.status(403).json({ success: false, error: 'You can only create screeners for yourself.' });
      }
    } else {
      if (!bodyStudentId) return res.status(400).json({ success: false, error: 'student_id is required.' });
      studentId = bodyStudentId;
      assignedBy = req.user?.userId || null;
    }

    // 2-week limit unless override
    if (!overrideFrequency) {
      const recent = await schoolPool.query(
        `SELECT id, completed_at FROM screener_instances
         WHERE student_id = $1 AND screener_type = $2 AND status = 'completed' AND completed_at IS NOT NULL
         AND completed_at >= (CURRENT_TIMESTAMP - INTERVAL '14 days')
         ORDER BY completed_at DESC LIMIT 1`,
        [studentId, normalizedType]
      );
      if (recent.rows.length > 0) {
        return res.status(409).json({
          success: false,
          error: 'Screener already completed within the last 2 weeks.',
          data: { last_completed_instance_id: recent.rows[0].id, last_completed_at: recent.rows[0].completed_at }
        });
      }
    }

    const trigger = (req.user.role === 'student') ? 'manual' : ((triggerSource && ['manual', 'scheduled', 'trigger'].includes(triggerSource)) ? triggerSource : 'manual');
    const result = await schoolPool.query(
      `INSERT INTO screener_instances (student_id, screener_type, status, trigger_source, assigned_by)
       VALUES ($1, $2, 'assigned', $3, $4) RETURNING *`,
      [studentId, normalizedType, trigger, assignedBy]
    );
    const row = result.rows[0];
    res.status(201).json({ success: true, data: row });
  } catch (err) {
    console.error('Create screener instance error:', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

// --- List instances for a student (student: own only; staff: by student_id)
router.get('/screeners/instances', authenticateToken, routeToSchoolDatabase, async (req, res) => {
  try {
    const { status, student_id: queryStudentId } = req.query;
    const schoolPool = req.db || (req.user?.schoolName ? getPool(req.user.schoolName) : null);
    if (!schoolPool) {
      return res.status(500).json({ success: false, error: 'Database not available.' });
    }

    await ensureScreenerTables(schoolPool);

    let studentId = queryStudentId ? parseInt(queryStudentId, 10) : null;
    if (req.user.role === 'student') {
      const me = await schoolPool.query(
        'SELECT id FROM students WHERE user_id = $1',
        [req.user.userId]
      );
      if (me.rows.length === 0) {
        console.warn(`[screeners/instances] No student profile for user_id=${req.user.userId}; returning empty list. Add a students row linked to this user.`);
        return res.json({ success: true, data: [] });
      }
      studentId = me.rows[0].id;
    } else if (!studentId) {
      return res.status(400).json({ success: false, error: 'student_id required for non-student users.' });
    }

    let sql = 'SELECT * FROM screener_instances WHERE student_id = $1';
    const params = [studentId];
    if (status) {
      sql += ' AND status = $2';
      params.push(status);
    }
    sql += ' ORDER BY created_at DESC';

    let result;
    try {
      result = await schoolPool.query(sql, params);
    } catch (tableErr) {
      if (tableErr.code === '42P01') {
        return res.json({ success: true, data: [] });
      }
      throw tableErr;
    }
    // Normalize screener_type to lowercase so staff-assigned PHQ-9/GAD-7 matches catalog in app
    const rows = result.rows.map(r => ({
      ...r,
      screener_type: r.screener_type ? String(r.screener_type).trim().toLowerCase() : r.screener_type
    }));
    res.json({ success: true, data: rows });
  } catch (err) {
    console.error('List screener instances error:', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

// --- Get one instance (student: own only)
router.get('/screeners/instances/:id', authenticateToken, routeToSchoolDatabase, async (req, res) => {
  try {
    const id = parseInt(req.params.id, 10);
    const { student_id: queryStudentId } = req.query;
    const schoolPool = req.db || (req.user?.schoolName ? getPool(req.user.schoolName) : null);
    if (!schoolPool) return res.status(500).json({ success: false, error: 'Database not available.' });

    let studentId = queryStudentId ? parseInt(queryStudentId, 10) : null;
    if (req.user.role === 'student') {
      const me = await schoolPool.query('SELECT id FROM students WHERE user_id = $1', [req.user.userId]);
      if (me.rows.length === 0) return res.status(404).json({ success: false, error: 'Student profile not found.' });
      studentId = me.rows[0].id;
    }

    let sql = 'SELECT * FROM screener_instances WHERE id = $1';
    const params = [id];
    if (studentId != null) {
      sql += ' AND student_id = $2';
      params.push(studentId);
    }
    const result = await schoolPool.query(sql, params);
    if (result.rows.length === 0) return res.status(404).json({ success: false, error: 'Instance not found.' });
    const row = result.rows[0];
    if (row.screener_type) row.screener_type = String(row.screener_type).trim().toLowerCase();
    res.json({ success: true, data: row });
  } catch (err) {
    console.error('Get screener instance error:', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

// --- Submit responses (student only, own instance)
router.post('/screeners/instances/:id/submit', authenticateToken, requireRole('student'), routeToSchoolDatabase, async (req, res) => {
  try {
    const id = parseInt(req.params.id, 10);
    const { student_id: bodyStudentId, responses } = req.body;
    const schoolPool = req.db || (req.user?.schoolName ? getPool(req.user.schoolName) : null);
    if (!schoolPool) return res.status(500).json({ success: false, error: 'Database not available.' });

    await ensureScreenerTables(schoolPool);

    const me = await schoolPool.query('SELECT id FROM students WHERE user_id = $1', [req.user.userId]);
    if (me.rows.length === 0) return res.status(404).json({ success: false, error: 'Student profile not found.' });
    const studentId = me.rows[0].id;
    if (bodyStudentId != null && Number(bodyStudentId) !== studentId) {
      return res.status(403).json({ success: false, error: 'Forbidden.' });
    }

    const inst = await schoolPool.query(
      'SELECT * FROM screener_instances WHERE id = $1 AND student_id = $2',
      [id, studentId]
    );
    if (inst.rows.length === 0) return res.status(404).json({ success: false, error: 'Instance not found.' });
    const instance = inst.rows[0];
    if (instance.status === 'completed') {
      return res.status(409).json({ success: false, error: 'Instance already completed.' });
    }

    const type = String(instance.screener_type || '').toLowerCase();
    const expectedLen = type === 'phq9' ? 9 : 7;
    const ans = Array(expectedLen).fill(0);
    (responses || []).forEach(r => {
      const i = parseInt(r.question_index, 10);
      if (i >= 0 && i < expectedLen) ans[i] = Math.max(0, Math.min(3, parseInt(r.answer_value, 10) || 0));
    });

    try { await ensureScreenerSchemaFix(schoolPool); } catch (_) { /* use schema from ensureScreenerTables */ }

    await schoolPool.query('DELETE FROM screener_responses WHERE instance_id = $1', [id]);
    for (let i = 0; i < ans.length; i++) {
      await schoolPool.query(
        'INSERT INTO screener_responses (instance_id, question_index, answer_value) VALUES ($1, $2, $3)',
        [id, i, ans[i]]
      );
    }

    const computed = type === 'phq9' ? scorePHQ9(ans) : scoreGAD7(ans);

    await schoolPool.query('DELETE FROM screener_scores WHERE instance_id = $1', [id]);
    await schoolPool.query(
      'INSERT INTO screener_scores (instance_id, total, severity, positive, details) VALUES ($1, $2, $3, $4, $5)',
      [id, computed.total, computed.severity, computed.positive, JSON.stringify(computed.details || {})]
    );

    await schoolPool.query(
      'UPDATE screener_instances SET status = $2, completed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = $1',
      [id, 'completed']
    );

    // Notify staff on positive screen (non-blocking: if notifications table/columns differ, submission still succeeds)
    if (computed.positive) {
      try {
        const studentRow = await schoolPool.query(
          'SELECT s.first_name, s.last_name FROM students s WHERE s.id = $1',
          [studentId]
        );
        const studentName = studentRow.rows[0] ? `${studentRow.rows[0].first_name || ''} ${studentRow.rows[0].last_name || ''}`.trim() : `Student #${studentId}`;
        const title = `Positive screener: ${type.toUpperCase()}`;
        const body = `Student ${studentName} completed ${type.toUpperCase()} with severity "${computed.severity}". Total score: ${computed.total}.`;
        const staff = await schoolPool.query(
          "SELECT id FROM users WHERE role IN ('staff', 'admin', 'associate', 'expert')"
        );
        for (const u of staff.rows) {
          await schoolPool.query(
            `INSERT INTO notifications (user_id, title, body, related_type, related_id, created_at)
             VALUES ($1, $2, $3, 'screener_instance', $4, CURRENT_TIMESTAMP)`,
            [u.id, title, body, id]
          );
        }
      } catch (notifErr) {
        console.error('Submit screener: positive notification skipped:', notifErr.message);
      }
    }

    const scoreRow = await schoolPool.query(
      'SELECT * FROM screener_scores WHERE instance_id = $1',
      [id]
    );
    res.json({
      success: true,
      data: {
        instance_id: id,
        status: 'completed',
        total: computed.total,
        severity: computed.severity,
        positive: computed.positive,
        score_row: scoreRow.rows[0] || null
      }
    });
  } catch (err) {
    console.error('Submit screener error:', err);
    res.status(500).json({
      success: false,
      error: 'Submission failed. Please try again or contact support.'
    });
  }
});

// --- Student report: latest by type + history
router.get('/screeners/reports/student/:studentId', authenticateToken, routeToSchoolDatabase, async (req, res) => {
  try {
    const studentId = parseInt(req.params.studentId, 10);
    const schoolPool = req.db || (req.user?.schoolName ? getPool(req.user.schoolName) : null);
    if (!schoolPool) return res.status(500).json({ success: false, error: 'Database not available.' });

    if (req.user.role === 'student') {
      const me = await schoolPool.query('SELECT id FROM students WHERE user_id = $1', [req.user.userId]);
      if (me.rows.length === 0) return res.status(404).json({ success: false, error: 'Student profile not found.' });
      if (me.rows[0].id !== studentId) return res.status(403).json({ success: false, error: 'Forbidden.' });
    }

    const rows = await schoolPool.query(
      `SELECT si.*, ss.total, ss.severity, ss.positive
       FROM screener_instances si
       LEFT JOIN screener_scores ss ON ss.instance_id = si.id
       WHERE si.student_id = $1
       ORDER BY si.completed_at DESC NULLS LAST, si.created_at DESC
       LIMIT 200`,
      [studentId]
    );

    const latestByType = {};
    rows.rows.forEach(r => {
      const t = String(r.screener_type || '').toLowerCase();
      if (!latestByType[t]) latestByType[t] = r;
    });

    res.json({
      success: true,
      data: {
        studentId,
        latestByType,
        history: rows.rows
      }
    });
  } catch (err) {
    console.error('Student screener report error:', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

// --- School aggregate report (counts by type+severity, last 180 days)
router.get('/screeners/reports/school', authenticateToken, requireRole('associate', 'staff', 'admin', 'expert'), routeToSchoolDatabase, async (req, res) => {
  try {
    const schoolPool = req.db || (req.user?.schoolName ? getPool(req.user.schoolName) : null);
    if (!schoolPool) return res.status(500).json({ success: false, error: 'Database not available.' });

    const rows = await schoolPool.query(
      `SELECT si.screener_type, ss.severity, COUNT(*) AS count
       FROM screener_instances si
       JOIN screener_scores ss ON ss.instance_id = si.id
       WHERE si.status = 'completed' AND si.completed_at >= (CURRENT_TIMESTAMP - INTERVAL '180 days')
       GROUP BY si.screener_type, ss.severity
       ORDER BY si.screener_type, ss.severity`
    );

    const byType = {};
    rows.rows.forEach(r => {
      const t = String(r.screener_type || '').toLowerCase();
      if (!byType[t]) byType[t] = {};
      byType[t][r.severity] = parseInt(r.count, 10);
    });

    res.json({
      success: true,
      data: {
        byType,
        raw: rows.rows
      }
    });
  } catch (err) {
    console.error('School screener report error:', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

module.exports = router;
