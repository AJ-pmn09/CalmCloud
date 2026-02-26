const express = require('express');
const { authenticateToken, requireRole } = require('../middleware/auth');
const { routeToSchoolDatabase } = require('../middleware/databaseRouter');
const { getPool } = require('../config/databaseManager');

const router = express.Router();

// Get trends data for staff/associate analytics (optional studentIds for peer comparison)
router.get('/analytics/trends', authenticateToken, requireRole('associate', 'expert', 'staff', 'admin'), routeToSchoolDatabase, async (req, res) => {
  try {
    const { days = 7, studentIds } = req.query;
    const schoolName = req.user.schoolName;
    const schoolPool = req.db || (schoolName ? getPool(schoolName) : null);
    
    if (!schoolPool) {
      return res.status(500).json({ error: 'Database connection error' });
    }

    const daysInt = parseInt(days) || 7;
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - daysInt);
    const startDateStr = startDate.toISOString().split('T')[0];

    // Optional: filter by selected student IDs for peer comparison
    let studentIdsArray = null;
    if (studentIds && typeof studentIds === 'string' && studentIds.trim()) {
      studentIdsArray = studentIds.split(',').map(id => parseInt(id.trim(), 10)).filter(n => !isNaN(n));
    }

    const stressWhere = studentIdsArray && studentIdsArray.length > 0
      ? 'date >= $1::date AND (stress_level IS NOT NULL OR mood_rating IS NOT NULL) AND student_id = ANY($2::int[])'
      : 'date >= $1::date AND (stress_level IS NOT NULL OR mood_rating IS NOT NULL)';
    const activityWhere = studentIdsArray && studentIdsArray.length > 0
      ? 'date >= $1::date AND student_id = ANY($2::int[])'
      : 'date >= $1::date';

    const stressParams = studentIdsArray && studentIdsArray.length > 0 ? [startDateStr, studentIdsArray] : [startDateStr];
    const activityParams = studentIdsArray && studentIdsArray.length > 0 ? [startDateStr, studentIdsArray] : [startDateStr];

    console.log(`[Analytics Trends] Querying from ${startDateStr} (${daysInt} days ago) for school: ${schoolName}, studentIds: ${studentIdsArray ? studentIdsArray.join(',') : 'all'}`);

    // Get stress trends - average stress per day (use COALESCE(stress_level, mood_rating) so mood-only check-ins count for peer comparison)
    const stressTrendsResult = await schoolPool.query(
      `SELECT 
        date,
        AVG(COALESCE(stress_level, mood_rating))::numeric(10,2) as avg_stress,
        COUNT(*) as checkin_count
       FROM daily_checkins
       WHERE ${stressWhere}
       GROUP BY date
       ORDER BY date ASC`,
      stressParams
    );

    console.log(`[Analytics Trends] Found ${stressTrendsResult.rows.length} days with stress data`);

    // Get check-in activity - count per day (filtered by studentIds when provided)
    const activityTrendsResult = await schoolPool.query(
      `SELECT 
        date,
        COUNT(*) as checkin_count
       FROM daily_checkins
       WHERE ${activityWhere}
       GROUP BY date
       ORDER BY date ASC`,
      activityParams
    );

    console.log(`[Analytics Trends] Found ${activityTrendsResult.rows.length} days with check-in activity`);

    // Also include activity_logs (stress_level and activity count) so student data from both tables is reflected
    const stressLogsWhere = studentIdsArray && studentIdsArray.length > 0
      ? 'date >= $1::date AND stress_level IS NOT NULL AND student_id = ANY($2::int[])'
      : 'date >= $1::date AND stress_level IS NOT NULL';
    const activityLogsWhere = studentIdsArray && studentIdsArray.length > 0
      ? 'date >= $1::date AND student_id = ANY($2::int[])'
      : 'date >= $1::date';
    const logsParams = studentIdsArray && studentIdsArray.length > 0 ? [startDateStr, studentIdsArray] : [startDateStr];

    const stressFromLogs = await schoolPool.query(
      `SELECT date, AVG(stress_level)::numeric(10,2) as avg_stress, COUNT(*) as cnt
       FROM activity_logs WHERE ${stressLogsWhere} GROUP BY date ORDER BY date ASC`,
      logsParams
    );
    const activityFromLogs = await schoolPool.query(
      `SELECT date, COUNT(*) as checkin_count FROM activity_logs WHERE ${activityLogsWhere} GROUP BY date ORDER BY date ASC`,
      logsParams
    );

    // Generate date range
    const dates = [];
    const calendar = new Date();
    for (let i = daysInt - 1; i >= 0; i--) {
      const date = new Date(calendar);
      date.setDate(date.getDate() - i);
      dates.push(date.toISOString().split('T')[0]);
    }

    // Map stress data
    const stressMap = {};
    stressTrendsResult.rows.forEach(row => {
      let dateStr;
      if (row.date instanceof Date) {
        dateStr = row.date.toISOString().split('T')[0];
      } else if (typeof row.date === 'string') {
        dateStr = row.date.split('T')[0];
      } else {
        // Handle PostgreSQL date type
        dateStr = row.date.toISOString ? row.date.toISOString().split('T')[0] : String(row.date).split('T')[0];
      }
      const avgStress = parseFloat(row.avg_stress);
      if (!isNaN(avgStress) && avgStress > 0) {
        stressMap[dateStr] = avgStress;
      }
    });

    // Map activity data
    const activityMap = {};
    activityTrendsResult.rows.forEach(row => {
      let dateStr;
      if (row.date instanceof Date) {
        dateStr = row.date.toISOString().split('T')[0];
      } else if (typeof row.date === 'string') {
        dateStr = row.date.split('T')[0];
      } else {
        // Handle PostgreSQL date type
        dateStr = row.date.toISOString ? row.date.toISOString().split('T')[0] : String(row.date).split('T')[0];
      }
      const count = parseInt(row.checkin_count);
      if (!isNaN(count) && count > 0) {
        activityMap[dateStr] = count;
      }
    });

    // Merge activity_logs stress (fill dates with no daily_checkins stress)
    stressFromLogs.rows.forEach(row => {
      let dateStr;
      if (row.date instanceof Date) dateStr = row.date.toISOString().split('T')[0];
      else if (typeof row.date === 'string') dateStr = row.date.split('T')[0];
      else dateStr = row.date.toISOString ? row.date.toISOString().split('T')[0] : String(row.date).split('T')[0];
      const avgStress = parseFloat(row.avg_stress);
      if (!isNaN(avgStress) && avgStress > 0 && !stressMap[dateStr]) {
        stressMap[dateStr] = avgStress;
      }
    });

    // Merge activity_logs activity count
    activityFromLogs.rows.forEach(row => {
      let dateStr;
      if (row.date instanceof Date) dateStr = row.date.toISOString().split('T')[0];
      else if (typeof row.date === 'string') dateStr = row.date.split('T')[0];
      else dateStr = row.date.toISOString ? row.date.toISOString().split('T')[0] : String(row.date).split('T')[0];
      const count = parseInt(row.checkin_count);
      if (!isNaN(count) && count > 0) {
        activityMap[dateStr] = (activityMap[dateStr] || 0) + count;
      }
    });

    // Build response arrays
    const stressTrends = dates.map(date => stressMap[date] || 0);
    const activityTrends = dates.map(date => activityMap[date] || 0);

    // Debug: Log sample data
    if (stressTrendsResult.rows.length > 0) {
      console.log(`[Analytics Trends] Sample stress data:`, stressTrendsResult.rows.slice(0, 3).map(r => ({
        date: r.date,
        avg_stress: r.avg_stress,
        count: r.checkin_count
      })));
    }
    if (activityTrendsResult.rows.length > 0) {
      console.log(`[Analytics Trends] Sample activity data:`, activityTrendsResult.rows.slice(0, 3).map(r => ({
        date: r.date,
        count: r.checkin_count
      })));
    }

    console.log(`[Analytics Trends] School: ${schoolName}, Days: ${daysInt}, Stress data points: ${stressTrendsResult.rows.length}, Activity data points: ${activityTrendsResult.rows.length}`);
    console.log(`[Analytics Trends] Mapped stress dates: ${Object.keys(stressMap).length}, Mapped activity dates: ${Object.keys(activityMap).length}`);
    console.log(`[Analytics Trends] Final stressTrends array length: ${stressTrends.length}, non-zero values: ${stressTrends.filter(v => v > 0).length}`);
    console.log(`[Analytics Trends] Final activityTrends array length: ${activityTrends.length}, non-zero values: ${activityTrends.filter(v => v > 0).length}`);

    // Peer comparison: per-student metrics when studentIds provided (in-range + latest ever for display)
    let peerComparison = null;
    let studentTrends = null;
    if (studentIdsArray && studentIdsArray.length > 0) {
      const peerResult = await schoolPool.query(
        `SELECT s.id as student_id,
                COALESCE(NULLIF(TRIM(u.first_name || ' ' || u.last_name), ''), u.email, 'Student') as student_name,
                (SELECT stress_level FROM daily_checkins dc WHERE dc.student_id = s.id AND dc.date >= $1::date ORDER BY dc.date DESC LIMIT 1) as last_stress,
                (SELECT mood_rating FROM daily_checkins dc WHERE dc.student_id = s.id AND dc.date >= $1::date ORDER BY dc.date DESC LIMIT 1) as last_mood,
                (SELECT COUNT(*) FROM daily_checkins dc WHERE dc.student_id = s.id AND dc.date >= $1::date) as checkin_count,
                (SELECT stress_level FROM daily_checkins dc WHERE dc.student_id = s.id ORDER BY dc.date DESC LIMIT 1) as latest_stress_any,
                (SELECT mood_rating FROM daily_checkins dc WHERE dc.student_id = s.id ORDER BY dc.date DESC LIMIT 1) as latest_mood_any,
                (SELECT date FROM daily_checkins dc WHERE dc.student_id = s.id ORDER BY dc.date DESC LIMIT 1) as latest_checkin_date
         FROM students s
         JOIN users u ON s.user_id = u.id
         WHERE s.id = ANY($2::int[])`,
        [startDateStr, studentIdsArray]
      );
      peerComparison = peerResult.rows.map(r => {
        const inRangeStress = r.last_stress != null ? parseInt(r.last_stress, 10) : null;
        const inRangeMood = r.last_mood != null ? parseInt(r.last_mood, 10) : null;
        const latestStress = r.latest_stress_any != null ? parseInt(r.latest_stress_any, 10) : inRangeStress;
        const latestMood = r.latest_mood_any != null ? parseInt(r.latest_mood_any, 10) : inRangeMood;
        const latestDate = r.latest_checkin_date ? (r.latest_checkin_date.toISOString ? r.latest_checkin_date.toISOString().split('T')[0] : String(r.latest_checkin_date).split('T')[0]) : null;
        return {
          studentId: r.student_id,
          studentName: r.student_name,
          lastStress: inRangeStress,
          lastMood: inRangeMood,
          checkinCount: parseInt(r.checkin_count, 10) || 0,
          latestStressAny: latestStress,
          latestMoodAny: latestMood,
          latestCheckinDate: latestDate
        };
      });

      // Per-student stress and activity by day (one line per student for charts)
      const perStudentStress = await schoolPool.query(
        `SELECT student_id, date, AVG(COALESCE(stress_level, mood_rating))::numeric(10,2) as avg_stress
         FROM daily_checkins
         WHERE date >= $1::date AND student_id = ANY($2::int[])
         GROUP BY student_id, date
         ORDER BY student_id, date`,
        [startDateStr, studentIdsArray]
      );
      const perStudentActivity = await schoolPool.query(
        `SELECT student_id, date, COUNT(*) as checkin_count
         FROM daily_checkins
         WHERE date >= $1::date AND student_id = ANY($2::int[])
         GROUP BY student_id, date
         ORDER BY student_id, date`,
        [startDateStr, studentIdsArray]
      );
      const stressByStudentDate = {};
      perStudentStress.rows.forEach(row => {
        const sid = row.student_id;
        const dateStr = row.date && (row.date.toISOString ? row.date.toISOString().split('T')[0] : String(row.date).split('T')[0]);
        const v = parseFloat(row.avg_stress);
        if (!stressByStudentDate[sid]) stressByStudentDate[sid] = {};
        if (!isNaN(v) && v > 0) stressByStudentDate[sid][dateStr] = v;
      });
      const activityByStudentDate = {};
      perStudentActivity.rows.forEach(row => {
        const sid = row.student_id;
        const dateStr = row.date && (row.date.toISOString ? row.date.toISOString().split('T')[0] : String(row.date).split('T')[0]);
        const v = parseInt(row.checkin_count, 10) || 0;
        if (!activityByStudentDate[sid]) activityByStudentDate[sid] = {};
        if (v > 0) activityByStudentDate[sid][dateStr] = v;
      });
      studentTrends = peerComparison.map(peer => ({
        studentId: peer.studentId,
        studentName: peer.studentName,
        stressByDay: dates.map(d => stressByStudentDate[peer.studentId] && stressByStudentDate[peer.studentId][d] != null
          ? parseFloat(stressByStudentDate[peer.studentId][d]) : 0),
        activityByDay: dates.map(d => activityByStudentDate[peer.studentId] && activityByStudentDate[peer.studentId][d] != null
          ? parseFloat(activityByStudentDate[peer.studentId][d]) : 0)
      }));
    }

    const response = {
      success: true,
      days: daysInt,
      stressTrends,
      activityTrends,
      dates,
      meta: {
        stressDataPoints: stressTrendsResult.rows.length,
        activityDataPoints: activityTrendsResult.rows.length,
        mappedStressDates: Object.keys(stressMap).length,
        mappedActivityDates: Object.keys(activityMap).length,
        startDate: startDateStr,
        endDate: new Date().toISOString().split('T')[0]
      }
    };
    if (peerComparison) response.peerComparison = peerComparison;
    if (studentTrends) response.studentTrends = studentTrends;
    res.json(response);
  } catch (error) {
    console.error('Get analytics trends error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

// Diagnostic endpoint to check if data exists
router.get('/analytics/diagnostic', authenticateToken, requireRole('associate', 'expert', 'staff', 'admin'), routeToSchoolDatabase, async (req, res) => {
  try {
    const schoolName = req.user.schoolName;
    const schoolPool = req.db || (schoolName ? getPool(schoolName) : null);
    
    if (!schoolPool) {
      return res.status(500).json({ error: 'Database connection error' });
    }

    // Check total check-ins
    const totalCheckins = await schoolPool.query(
      'SELECT COUNT(*) as count FROM daily_checkins',
      []
    );

    // Check check-ins with stress data
    const stressCheckins = await schoolPool.query(
      'SELECT COUNT(*) as count FROM daily_checkins WHERE stress_level IS NOT NULL',
      []
    );

    // Get recent check-ins sample
    const recentCheckins = await schoolPool.query(
      `SELECT date, stress_level, mood_rating, student_id 
       FROM daily_checkins 
       WHERE date >= CURRENT_DATE - INTERVAL '30 days'
       ORDER BY date DESC 
       LIMIT 10`,
      []
    );

    // Get date range of check-ins
    const dateRange = await schoolPool.query(
      `SELECT MIN(date) as min_date, MAX(date) as max_date, COUNT(DISTINCT date) as unique_dates
       FROM daily_checkins`,
      []
    );

    res.json({
      success: true,
      school: schoolName,
      totalCheckins: parseInt(totalCheckins.rows[0].count),
      stressCheckins: parseInt(stressCheckins.rows[0].count),
      dateRange: dateRange.rows[0],
      recentCheckins: recentCheckins.rows.map(r => ({
        date: r.date,
        stress_level: r.stress_level,
        mood_rating: r.mood_rating,
        student_id: r.student_id
      }))
    });
  } catch (error) {
    console.error('Analytics diagnostic error:', error);
    res.status(500).json({ error: 'Server error', details: error.message });
  }
});

module.exports = router;
