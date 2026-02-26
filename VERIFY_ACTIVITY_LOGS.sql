-- Verification script to check 30-day activity logs data
-- Run this on your database to verify data exists

-- Replace STUDENT_ID with actual student ID, or use this to find it:
-- SELECT id, first_name, last_name FROM students;

-- 1. Check total activity logs for a student
SELECT 
    COUNT(*) as total_logs,
    MIN(date) as earliest_date,
    MAX(date) as latest_date,
    CURRENT_DATE - MIN(date) as days_oldest,
    CURRENT_DATE - MAX(date) as days_newest
FROM activity_logs
WHERE student_id = 1; -- Replace with actual student_id

-- 2. Check logs in last 30 days
SELECT 
    COUNT(*) as logs_last_30_days,
    MIN(date) as earliest_in_range,
    MAX(date) as latest_in_range
FROM activity_logs
WHERE student_id = 1 -- Replace with actual student_id
  AND date >= CURRENT_DATE - INTERVAL '30 days';

-- 3. Sample of recent logs
SELECT 
    date,
    steps,
    sleep_hours,
    hydration_percent,
    nutrition_percent
FROM activity_logs
WHERE student_id = 1 -- Replace with actual student_id
  AND date >= CURRENT_DATE - INTERVAL '30 days'
ORDER BY date DESC
LIMIT 10;

-- 4. Check for missing data (dates with no logs in last 30 days)
WITH date_series AS (
    SELECT generate_series(
        CURRENT_DATE - INTERVAL '30 days',
        CURRENT_DATE,
        '1 day'::interval
    )::date AS date
)
SELECT 
    ds.date,
    CASE WHEN al.id IS NULL THEN 'MISSING' ELSE 'EXISTS' END as status
FROM date_series ds
LEFT JOIN activity_logs al ON al.date = ds.date AND al.student_id = 1 -- Replace with actual student_id
ORDER BY ds.date DESC;

-- 5. Summary by student (if you want to check all students)
SELECT 
    s.id,
    s.first_name || ' ' || s.last_name as student_name,
    COUNT(al.id) as total_logs,
    COUNT(CASE WHEN al.date >= CURRENT_DATE - INTERVAL '30 days' THEN 1 END) as logs_last_30_days,
    MIN(al.date) as earliest_log,
    MAX(al.date) as latest_log
FROM students s
LEFT JOIN activity_logs al ON s.id = al.student_id
GROUP BY s.id, s.first_name, s.last_name
ORDER BY logs_last_30_days DESC;
