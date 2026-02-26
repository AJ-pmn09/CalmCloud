-- Production-Ready Sample Data Seeding Script
-- This script creates comprehensive, realistic data for all roles with various scenarios
-- Data includes check-ins, requests, alerts, logs, and more over the past 30 days

BEGIN;

-- ============================================
-- 1. DAILY CHECK-INS (Past 30 days for all students)
-- ============================================
-- Create check-ins with various emotions, stress levels, and scenarios

INSERT INTO daily_checkins (student_id, date, stress_level, mood_rating, emotion, emotion_intensity, stress_source, additional_notes)
SELECT 
    s.id,
    CURRENT_DATE - (30 - row_number() OVER (PARTITION BY s.id ORDER BY random())),
    CASE 
        WHEN random() < 0.3 THEN floor(random() * 3 + 1)::int  -- Low stress (1-3)
        WHEN random() < 0.7 THEN floor(random() * 4 + 4)::int  -- Medium stress (4-7)
        ELSE floor(random() * 3 + 8)::int  -- High stress (8-10)
    END,
    CASE 
        WHEN random() < 0.2 THEN 1  -- Very low mood
        WHEN random() < 0.5 THEN 2  -- Low mood
        WHEN random() < 0.8 THEN 3  -- Neutral
        WHEN random() < 0.95 THEN 4  -- Good mood
        ELSE 5  -- Great mood
    END,
    (ARRAY['happy', 'calm', 'okay', 'sad', 'anxious', 'stressed'])[floor(random() * 6 + 1)::int],
    floor(random() * 10 + 1)::int,
    CASE 
        WHEN random() < 0.3 THEN NULL
        WHEN random() < 0.5 THEN 'School work'
        WHEN random() < 0.7 THEN 'Social relationships'
        WHEN random() < 0.85 THEN 'Family issues'
        WHEN random() < 0.95 THEN 'Health concerns'
        ELSE 'Future planning'
    END,
    CASE 
        WHEN random() < 0.7 THEN NULL
        ELSE 'Had a ' || (ARRAY['good', 'challenging', 'busy', 'relaxing'])[floor(random() * 4 + 1)::int] || ' day'
    END
FROM students s
CROSS JOIN generate_series(1, 25) gs  -- 25 check-ins per student over past 30 days
WHERE random() > 0.1;  -- 90% of students have check-ins

-- ============================================
-- 2. ACTIVITY LOGS (Past 30 days)
-- ============================================
INSERT INTO activity_logs (student_id, date, steps, sleep_hours, hydration_percent, nutrition_percent, stress_level, heart_rate, mood, notes)
SELECT 
    s.id,
    CURRENT_DATE - (30 - gs)::int,
    floor(random() * 8000 + 2000)::int,  -- 2000-10000 steps
    round((random() * 4 + 6)::numeric, 2),  -- 6-10 hours sleep
    floor(random() * 40 + 50)::int,  -- 50-90% hydration
    floor(random() * 30 + 60)::int,  -- 60-90% nutrition
    floor(random() * 10 + 1)::int,  -- Stress 1-10
    floor(random() * 40 + 60)::int,  -- Heart rate 60-100
    (ARRAY['happy', 'calm', 'okay', 'sad', 'anxious'])[floor(random() * 5 + 1)::int],
    CASE WHEN random() < 0.3 THEN 'Regular activity day' ELSE NULL END
FROM students s
CROSS JOIN generate_series(1, 20) gs;  -- 20 activity logs per student

-- ============================================
-- 3. WEARABLE DATA (Past 30 days)
-- ============================================
INSERT INTO wearable_data (student_id, date, heart_rate, temperature, steps, sleep_hours)
SELECT 
    s.id,
    CURRENT_DATE - (30 - gs)::int,
    floor(random() * 50 + 60)::int,  -- Heart rate 60-110
    round((random() * 2 + 36.5)::numeric, 2),  -- Temperature 36.5-38.5
    floor(random() * 10000 + 3000)::int,  -- Steps 3000-13000
    round((random() * 5 + 5)::numeric, 2)  -- Sleep 5-10 hours
FROM students s
CROSS JOIN generate_series(1, 28) gs;  -- Daily wearable data

-- ============================================
-- 4. WELLNESS SCORES (Past 30 days)
-- ============================================
INSERT INTO wellness_scores (student_id, score, date, sentiment)
SELECT 
    s.id,
    floor(random() * 40 + 50)::int,  -- Score 50-90
    CURRENT_DATE - (30 - gs)::int,
    (ARRAY['positive', 'neutral', 'negative'])[floor(random() * 3 + 1)::int]
FROM students s
CROSS JOIN generate_series(1, 15) gs;  -- 15 wellness scores per student

-- ============================================
-- 5. ASSISTANCE REQUESTS (Various scenarios)
-- ============================================
-- Get parent and student IDs
DO $$
DECLARE
    parent_sarah_id INT;
    parent_david_id INT;
    parent_maria_id INT;
    student_emma_id INT;
    student_olivia_id INT;
    student_ethan_id INT;
    student_mia_id INT;
    student_james_id INT;
    student_sophia_id INT;
    student_michael_id INT;
    student_noah_id INT;
    student_ava_id INT;
    student_liam_id INT;
    student_isabella_id INT;
    student_lucas_id INT;
    associate_jennifer_id INT;
    associate_michael_id INT;
    associate_lisa_id INT;
BEGIN
    -- Get IDs
    SELECT id INTO parent_sarah_id FROM users WHERE email = 'sarah.thompson@email.com';
    SELECT id INTO parent_david_id FROM users WHERE email = 'david.wilson@email.com';
    SELECT id INTO parent_maria_id FROM users WHERE email = 'maria.martinez@email.com';
    
    SELECT s.id INTO student_emma_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'emma.thompson@horizon.edu';
    SELECT s.id INTO student_olivia_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'olivia.williams@houghton.edu';
    SELECT s.id INTO student_ethan_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'ethan.wilson@calumet.edu';
    SELECT s.id INTO student_mia_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'mia.moore@calumet.edu';
    SELECT s.id INTO student_james_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'james.martinez@horizon.edu';
    SELECT s.id INTO student_sophia_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'sophia.chen@horizon.edu';
    SELECT s.id INTO student_michael_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'michael.anderson@horizon.edu';
    SELECT s.id INTO student_noah_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'noah.brown@houghton.edu';
    SELECT s.id INTO student_ava_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'ava.davis@houghton.edu';
    SELECT s.id INTO student_liam_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'liam.garcia@houghton.edu';
    SELECT s.id INTO student_isabella_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'isabella.rodriguez@calumet.edu';
    SELECT s.id INTO student_lucas_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'lucas.taylor@calumet.edu';
    
    SELECT id INTO associate_jennifer_id FROM users WHERE email = 'jennifer.parker@school.edu';
    SELECT id INTO associate_michael_id FROM users WHERE email = 'michael.johnson@school.edu';
    SELECT id INTO associate_lisa_id FROM users WHERE email = 'lisa.martinez@school.edu';

    -- Scenario 1: Pending request (single child, normal priority)
    DECLARE
        request_emma_id INT;
    BEGIN
        INSERT INTO assistance_requests (student_id, parent_id, parent_name, message, urgency, status, request_type, created_at)
        VALUES (student_emma_id, parent_sarah_id, 'Sarah Thompson', 
                'I am concerned about Emma''s recent stress levels. She seems overwhelmed with schoolwork.', 
                'normal', 'pending', 'single', CURRENT_DATE - 2)
        RETURNING id INTO request_emma_id;
    END;
    
    -- Scenario 2: In-progress request (multiple children, high priority)
    DECLARE
        request_ethan_id INT;
    BEGIN
        INSERT INTO assistance_requests (student_id, parent_id, parent_name, message, urgency, status, request_type, handled_by, handled_by_name, handled_at, created_at)
        VALUES (student_ethan_id, parent_david_id, 'David Wilson', 
                'Both Ethan and Mia have been struggling with social interactions at school. Need guidance on how to support them.', 
                'high', 'in-progress', 'multiple', associate_michael_id, 'Michael Johnson', CURRENT_DATE - 1, CURRENT_DATE - 5)
        RETURNING id INTO request_ethan_id;
        
        INSERT INTO assistance_request_students (assistance_request_id, student_id)
        VALUES (request_ethan_id, student_ethan_id), (request_ethan_id, student_mia_id);
    END;
    
    -- Scenario 3: Resolved request (single child, low priority)
    INSERT INTO assistance_requests (student_id, parent_id, parent_name, message, urgency, status, request_type, handled_by, handled_by_name, handled_at, notes, created_at, updated_at)
    VALUES (student_james_id, parent_maria_id, 'Maria Martinez', 
            'James mentioned feeling anxious about upcoming exams. Would like some resources.', 
            'low', 'resolved', 'single', associate_jennifer_id, 'Jennifer Parker', CURRENT_DATE - 10, 
            'Provided study resources and stress management techniques. Follow-up scheduled.', CURRENT_DATE - 15, CURRENT_DATE - 8);
    
    -- Scenario 4: Multiple pending requests
    INSERT INTO assistance_requests (student_id, parent_id, parent_name, message, urgency, status, request_type, created_at)
    VALUES 
        (student_sophia_id, (SELECT id FROM users WHERE email = 'wei.chen@email.com'), 'Wei Chen',
         'Sophia has been very quiet lately. Concerned about her emotional well-being.', 'normal', 'pending', 'single', CURRENT_DATE - 3),
        (student_michael_id, (SELECT id FROM users WHERE email = 'robert.anderson@email.com'), 'Robert Anderson',
         'Michael needs support with time management and organization skills.', 'normal', 'pending', 'single', CURRENT_DATE - 1),
        (student_noah_id, (SELECT id FROM users WHERE email = 'jennifer.brown@email.com'), 'Jennifer Brown',
         'Noah seems to be having difficulty focusing in class. Requesting assessment.', 'high', 'pending', 'single', CURRENT_DATE - 4);
    
    -- Scenario 5: Resolved with notes
    INSERT INTO assistance_requests (student_id, parent_id, parent_name, message, urgency, status, request_type, handled_by, handled_by_name, handled_at, notes, created_at, updated_at)
    VALUES 
        (student_ava_id, (SELECT id FROM users WHERE email = 'christopher.davis@email.com'), 'Christopher Davis',
         'Ava has been experiencing sleep issues. Need recommendations.', 'normal', 'resolved', 'single',
         associate_lisa_id, 'Lisa Martinez', CURRENT_DATE - 7,
         'Recommended sleep hygiene practices and relaxation techniques. Parent will monitor progress.', CURRENT_DATE - 12, CURRENT_DATE - 6),
        (student_liam_id, (SELECT id FROM users WHERE email = 'patricia.garcia@email.com'), 'Patricia Garcia',
         'Liam needs support with peer relationships.', 'low', 'resolved', 'single',
         associate_michael_id, 'Michael Johnson', CURRENT_DATE - 14,
         'Conducted peer support group session. Positive progress observed.', CURRENT_DATE - 20, CURRENT_DATE - 13);
END $$;

-- ============================================
-- 6. EMERGENCY ALERTS (Various scenarios)
-- ============================================
DO $$
DECLARE
    student_emma_id INT;
    student_james_id INT;
    student_sophia_id INT;
    student_michael_id INT;
    student_noah_id INT;
    associate_jennifer_id INT;
    associate_michael_id INT;
    associate_lisa_id INT;
BEGIN
    SELECT s.id INTO student_emma_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'emma.thompson@horizon.edu';
    SELECT s.id INTO student_james_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'james.martinez@horizon.edu';
    SELECT s.id INTO student_sophia_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'sophia.chen@horizon.edu';
    SELECT s.id INTO student_michael_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'michael.anderson@horizon.edu';
    SELECT s.id INTO student_noah_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'noah.brown@houghton.edu';
    
    SELECT id INTO associate_jennifer_id FROM users WHERE email = 'jennifer.parker@school.edu';
    SELECT id INTO associate_michael_id FROM users WHERE email = 'michael.johnson@school.edu';
    SELECT id INTO associate_lisa_id FROM users WHERE email = 'lisa.martinez@school.edu';

    -- Scenario 1: Active emergency alert
    INSERT INTO emergency_alerts (student_id, alert_type, message, status, created_at)
    VALUES (student_emma_id, 'emergency', 'Student reported feeling extremely overwhelmed and unsafe. Immediate support needed.', 'active', CURRENT_TIMESTAMP - INTERVAL '2 hours');
    
    -- Scenario 2: Acknowledged alert
    INSERT INTO emergency_alerts (student_id, alert_type, message, status, acknowledged_by, acknowledged_at, created_at, updated_at)
    VALUES (student_james_id, 'urgent', 'Student expressed thoughts of self-harm. Crisis intervention initiated.', 'acknowledged', 
            associate_jennifer_id, CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '30 minutes');
    
    -- Scenario 3: Resolved alert
    INSERT INTO emergency_alerts (student_id, alert_type, message, status, acknowledged_by, acknowledged_at, resolved_by, resolved_at, resolution_notes, created_at, updated_at)
    VALUES (student_sophia_id, 'support', 'Student requested immediate counseling support due to family crisis.', 'resolved',
            associate_michael_id, CURRENT_TIMESTAMP - INTERVAL '2 days', associate_michael_id, CURRENT_TIMESTAMP - INTERVAL '1 day',
            'Provided crisis counseling. Connected with family resources. Follow-up scheduled.', CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '1 day');
    
    -- Scenario 4: Multiple active alerts
    INSERT INTO emergency_alerts (student_id, alert_type, message, status, created_at)
    VALUES 
        (student_michael_id, 'urgent', 'Student experiencing panic attack. Needs immediate assistance.', 'active', CURRENT_TIMESTAMP - INTERVAL '15 minutes'),
        (student_noah_id, 'support', 'Student feeling isolated and needs someone to talk to.', 'active', CURRENT_TIMESTAMP - INTERVAL '1 hour');
END $$;

-- ============================================
-- 7. FHIR OBSERVATIONS (Past 30 days)
-- ============================================
INSERT INTO fhir_observations (observation_id, student_id, loinc_code, loinc_display, value_quantity, value_string, value_unit, effective_date_time, subject_reference)
SELECT 
    'obs-' || s.id || '-' || generate_series,
    s.id,
    CASE (generate_series % 4)
        WHEN 0 THEN '75258-2'  -- Stress level
        WHEN 1 THEN '73985-4'  -- Heart rate
        WHEN 2 THEN '85354-9'  -- Sleep hours
        ELSE '39156-5'  -- Mood
    END,
    CASE (generate_series % 4)
        WHEN 0 THEN 'Stress Level'
        WHEN 1 THEN 'Heart Rate'
        WHEN 2 THEN 'Sleep Duration'
        ELSE 'Mood Assessment'
    END,
    CASE (generate_series % 4)
        WHEN 0 THEN floor(random() * 10 + 1)::numeric  -- Stress 1-10
        WHEN 1 THEN floor(random() * 50 + 60)::numeric  -- Heart rate 60-110
        WHEN 2 THEN round((random() * 5 + 5)::numeric, 2)  -- Sleep 5-10 hours
        ELSE NULL
    END,
    CASE (generate_series % 4)
        WHEN 3 THEN (ARRAY['happy', 'calm', 'okay', 'sad', 'anxious'])[floor(random() * 5 + 1)::int]
        ELSE NULL
    END,
    CASE (generate_series % 4)
        WHEN 1 THEN 'BPM'
        WHEN 2 THEN 'hours'
        ELSE NULL
    END,
    CURRENT_TIMESTAMP - (random() * INTERVAL '30 days'),
    'Patient/' || s.id
FROM students s
CROSS JOIN generate_series(1, 20);  -- 20 observations per student

-- ============================================
-- 8. COUNSELOR NOTES
-- ============================================
DO $$
DECLARE
    student_emma_id INT;
    student_james_id INT;
    student_sophia_id INT;
    student_michael_id INT;
    associate_jennifer_id INT;
    associate_michael_id INT;
    associate_lisa_id INT;
BEGIN
    SELECT s.id INTO student_emma_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'emma.thompson@horizon.edu';
    SELECT s.id INTO student_james_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'james.martinez@horizon.edu';
    SELECT s.id INTO student_sophia_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'sophia.chen@horizon.edu';
    SELECT s.id INTO student_michael_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'michael.anderson@horizon.edu';
    
    SELECT id INTO associate_jennifer_id FROM users WHERE email = 'jennifer.parker@school.edu';
    SELECT id INTO associate_michael_id FROM users WHERE email = 'michael.johnson@school.edu';
    SELECT id INTO associate_lisa_id FROM users WHERE email = 'lisa.martinez@school.edu';

    INSERT INTO counselor_notes (student_id, counselor_id, note_text, created_at)
    VALUES 
        (student_emma_id, associate_jennifer_id, 'Student shows signs of academic stress. Discussed time management strategies. Will follow up in 2 weeks.', CURRENT_DATE - 5),
        (student_james_id, associate_jennifer_id, 'Positive progress in managing anxiety. Using breathing techniques effectively. Continue current approach.', CURRENT_DATE - 3),
        (student_sophia_id, associate_michael_id, 'Initial assessment completed. Student expresses concerns about peer relationships. Group therapy recommended.', CURRENT_DATE - 7),
        (student_michael_id, associate_lisa_id, 'Student engaged well in session. Discussed coping strategies for test anxiety. Parent meeting scheduled.', CURRENT_DATE - 2),
        (student_emma_id, associate_jennifer_id, 'Follow-up session: Student reports improved sleep patterns. Stress levels decreasing. Continue monitoring.', CURRENT_DATE - 1);
END $$;

-- ============================================
-- 9. INTERVENTIONS
-- ============================================
DO $$
DECLARE
    student_emma_id INT;
    student_james_id INT;
    student_sophia_id INT;
    associate_jennifer_id INT;
    associate_michael_id INT;
BEGIN
    SELECT s.id INTO student_emma_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'emma.thompson@horizon.edu';
    SELECT s.id INTO student_james_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'james.martinez@horizon.edu';
    SELECT s.id INTO student_sophia_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'sophia.chen@horizon.edu';
    
    SELECT id INTO associate_jennifer_id FROM users WHERE email = 'jennifer.parker@school.edu';
    SELECT id INTO associate_michael_id FROM users WHERE email = 'michael.johnson@school.edu';

    INSERT INTO interventions (student_id, counselor_id, intervention_type, description, outcome, status, created_at)
    VALUES 
        (student_emma_id, associate_jennifer_id, 'Cognitive Behavioral Therapy', 
         'Weekly CBT sessions focusing on stress management and academic pressure.', 
         'Student showing improved coping mechanisms. Stress levels reduced by 30%.', 'active', CURRENT_DATE - 20),
        (student_james_id, associate_jennifer_id, 'Mindfulness Training', 
         'Daily mindfulness exercises and breathing techniques for anxiety management.', 
         'Significant improvement in anxiety symptoms. Student reports feeling more in control.', 'active', CURRENT_DATE - 15),
        (student_sophia_id, associate_michael_id, 'Group Therapy', 
         'Peer support group for social anxiety and relationship building.', 
         'Completed 8-week program. Student more confident in social situations.', 'completed', CURRENT_DATE - 60);
END $$;

-- ============================================
-- 10. APPOINTMENTS
-- ============================================
DO $$
DECLARE
    student_emma_id INT;
    student_james_id INT;
    student_sophia_id INT;
    student_michael_id INT;
    parent_sarah_id INT;
    parent_maria_id INT;
    associate_jennifer_id INT;
    associate_michael_id INT;
    associate_lisa_id INT;
BEGIN
    SELECT s.id INTO student_emma_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'emma.thompson@horizon.edu';
    SELECT s.id INTO student_james_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'james.martinez@horizon.edu';
    SELECT s.id INTO student_sophia_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'sophia.chen@horizon.edu';
    SELECT s.id INTO student_michael_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'michael.anderson@horizon.edu';
    
    SELECT id INTO parent_sarah_id FROM users WHERE email = 'sarah.thompson@email.com';
    SELECT id INTO parent_maria_id FROM users WHERE email = 'maria.martinez@email.com';
    
    SELECT id INTO associate_jennifer_id FROM users WHERE email = 'jennifer.parker@school.edu';
    SELECT id INTO associate_michael_id FROM users WHERE email = 'michael.johnson@school.edu';
    SELECT id INTO associate_lisa_id FROM users WHERE email = 'lisa.martinez@school.edu';

    INSERT INTO appointments (student_id, staff_id, parent_id, appointment_date, duration, type, notes, status, created_at)
    VALUES 
        (student_emma_id, associate_jennifer_id, parent_sarah_id, CURRENT_TIMESTAMP + INTERVAL '3 days', 45, 'counseling', 
         'Follow-up session to discuss progress on stress management strategies.', 'confirmed', CURRENT_DATE - 2),
        (student_james_id, associate_jennifer_id, parent_maria_id, CURRENT_TIMESTAMP + INTERVAL '1 week', 30, 'check-in', 
         'Regular check-in to monitor anxiety management progress.', 'pending', CURRENT_DATE - 1),
        (student_sophia_id, associate_michael_id, NULL, CURRENT_TIMESTAMP + INTERVAL '5 days', 60, 'group_therapy', 
         'Group therapy session for peer support.', 'confirmed', CURRENT_DATE - 3),
        (student_michael_id, associate_lisa_id, NULL, CURRENT_TIMESTAMP - INTERVAL '2 days', 30, 'counseling', 
         'Completed session on test anxiety management.', 'completed', CURRENT_DATE - 5);
END $$;

-- ============================================
-- 11. ACTIVITY TIMELINE
-- ============================================
DO $$
DECLARE
    student_emma_id INT;
    student_james_id INT;
    student_sophia_id INT;
    associate_jennifer_id INT;
    associate_michael_id INT;
BEGIN
    SELECT s.id INTO student_emma_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'emma.thompson@horizon.edu';
    SELECT s.id INTO student_james_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'james.martinez@horizon.edu';
    SELECT s.id INTO student_sophia_id FROM students s JOIN users u ON s.user_id = u.id WHERE u.email = 'sophia.chen@horizon.edu';
    
    SELECT id INTO associate_jennifer_id FROM users WHERE email = 'jennifer.parker@school.edu';
    SELECT id INTO associate_michael_id FROM users WHERE email = 'michael.johnson@school.edu';

    INSERT INTO activity_timeline (student_id, actor_id, action_type, summary, details, created_at)
    VALUES 
        (student_emma_id, student_emma_id, 'checkin_completed', 'Daily check-in submitted', '{"emotion": "anxious", "stress_level": 7}', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
        (student_emma_id, associate_jennifer_id, 'counselor_note_added', 'Counselor added note', '{"note_id": 1}', CURRENT_TIMESTAMP - INTERVAL '1 day'),
        (student_james_id, student_james_id, 'checkin_completed', 'Daily check-in submitted', '{"emotion": "calm", "stress_level": 3}', CURRENT_TIMESTAMP - INTERVAL '3 hours'),
        (student_james_id, associate_jennifer_id, 'intervention_started', 'Mindfulness training intervention started', '{"intervention_type": "Mindfulness Training"}', CURRENT_TIMESTAMP - INTERVAL '15 days'),
        (student_sophia_id, student_sophia_id, 'checkin_completed', 'Daily check-in submitted', '{"emotion": "okay", "stress_level": 5}', CURRENT_TIMESTAMP - INTERVAL '5 hours'),
        (student_sophia_id, associate_michael_id, 'appointment_scheduled', 'Group therapy appointment scheduled', '{"appointment_date": "2025-01-02"}', CURRENT_TIMESTAMP - INTERVAL '3 days'),
        (student_emma_id, associate_jennifer_id, 'alert_acknowledged', 'Emergency alert acknowledged', '{"alert_type": "emergency"}', CURRENT_TIMESTAMP - INTERVAL '2 hours');
END $$;

-- ============================================
-- 12. SYMPTOM LOGS
-- ============================================
INSERT INTO symptom_logs (student_id, date, symptom_type, severity, notes)
SELECT 
    s.id,
    CURRENT_DATE - (random() * 30)::int,
    (ARRAY['Headache', 'Fatigue', 'Anxiety', 'Sleep Issues', 'Loss of Appetite', 'Difficulty Concentrating'])[floor(random() * 6 + 1)::int],
    floor(random() * 5 + 1)::int,
    CASE WHEN random() < 0.5 THEN 'Reported during check-in' ELSE NULL END
FROM students s
CROSS JOIN generate_series(1, 5);  -- 5 symptom logs per student

-- ============================================
-- 13. AUDIT LOGS (Track important actions)
-- ============================================
INSERT INTO audit_logs (user_id, user_role, action_type, resource_type, resource_id, details, created_at)
SELECT 
    u.id,
    u.role,
    (ARRAY['checkin_created', 'request_created', 'alert_created', 'note_added', 'intervention_started'])[floor(random() * 5 + 1)::int],
    (ARRAY['checkin', 'request', 'alert', 'note', 'intervention'])[floor(random() * 5 + 1)::int],
    floor(random() * 100 + 1)::int,
    '{"action": "performed", "timestamp": "' || CURRENT_TIMESTAMP || '"}',
    CURRENT_TIMESTAMP - (random() * INTERVAL '30 days')
FROM users u
WHERE u.role IN ('student', 'parent', 'associate', 'expert')
LIMIT 50;

COMMIT;

-- ============================================
-- VERIFICATION QUERIES
-- ============================================
SELECT 'Daily Check-ins:' as type, COUNT(*)::text as count FROM daily_checkins
UNION ALL
SELECT 'Activity Logs:', COUNT(*)::text FROM activity_logs
UNION ALL
SELECT 'Wearable Data:', COUNT(*)::text FROM wearable_data
UNION ALL
SELECT 'Wellness Scores:', COUNT(*)::text FROM wellness_scores
UNION ALL
SELECT 'Assistance Requests:', COUNT(*)::text FROM assistance_requests
UNION ALL
SELECT 'Emergency Alerts:', COUNT(*)::text FROM emergency_alerts
UNION ALL
SELECT 'FHIR Observations:', COUNT(*)::text FROM fhir_observations
UNION ALL
SELECT 'Counselor Notes:', COUNT(*)::text FROM counselor_notes
UNION ALL
SELECT 'Interventions:', COUNT(*)::text FROM interventions
UNION ALL
SELECT 'Appointments:', COUNT(*)::text FROM appointments
UNION ALL
SELECT 'Activity Timeline:', COUNT(*)::text FROM activity_timeline
UNION ALL
SELECT 'Symptom Logs:', COUNT(*)::text FROM symptom_logs
UNION ALL
SELECT 'Audit Logs:', COUNT(*)::text FROM audit_logs;

