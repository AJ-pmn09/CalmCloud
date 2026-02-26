-- Create comprehensive data for the past week (7 days) for all students
-- This ensures graphs have data and everything is synced

BEGIN;

-- Delete old check-ins and create fresh week data
DELETE FROM daily_checkins WHERE date >= CURRENT_DATE - INTERVAL '7 days';
DELETE FROM fhir_observations WHERE effective_date_time >= CURRENT_TIMESTAMP - INTERVAL '7 days';
DELETE FROM activity_logs WHERE date >= CURRENT_DATE - INTERVAL '7 days';
DELETE FROM wellness_scores WHERE date >= CURRENT_DATE - INTERVAL '7 days';
DELETE FROM wearable_data WHERE date >= CURRENT_DATE - INTERVAL '7 days';

-- Function to create realistic week data for a student
DO $$
DECLARE
    student_rec RECORD;
    day_offset INT;
    emotion_val TEXT;
    stress_val INT;
    mood_val INT;
    heart_rate_val INT;
    water_val INT;
    steps_val INT;
    sleep_val NUMERIC;
    wellness_val INT;
BEGIN
    FOR student_rec IN SELECT s.id, s.first_name FROM students s LOOP
        -- Create different scenarios for different students
        -- Emma: Moderate stress, improving trend
        -- James: High stress, needs attention
        -- Sophia: Low stress, stable
        -- Others: Various patterns
        
        FOR day_offset IN 0..6 LOOP
            -- Determine values based on student and day
            IF student_rec.first_name = 'Emma' THEN
                -- Emma: Improving trend (stress decreasing)
                stress_val := 7 - day_offset; -- 7 down to 1
                mood_val := CASE 
                    WHEN day_offset <= 2 THEN 2
                    WHEN day_offset <= 4 THEN 3
                    ELSE 4
                END;
                emotion_val := CASE 
                    WHEN day_offset <= 1 THEN 'stressed'
                    WHEN day_offset <= 3 THEN 'anxious'
                    WHEN day_offset <= 5 THEN 'okay'
                    ELSE 'calm'
                END;
            ELSIF student_rec.first_name = 'James' THEN
                -- James: High stress, needs attention
                stress_val := CASE 
                    WHEN day_offset <= 2 THEN 9
                    WHEN day_offset <= 4 THEN 8
                    ELSE 7
                END;
                mood_val := 2;
                emotion_val := CASE 
                    WHEN day_offset <= 3 THEN 'stressed'
                    ELSE 'anxious'
                END;
            ELSIF student_rec.first_name = 'Sophia' THEN
                -- Sophia: Low stress, stable
                stress_val := 3 + (day_offset % 2); -- 3-4
                mood_val := 4;
                emotion_val := CASE 
                    WHEN day_offset % 2 = 0 THEN 'calm'
                    ELSE 'okay'
                END;
            ELSIF student_rec.first_name = 'Michael' THEN
                -- Michael: Fluctuating
                stress_val := 4 + (day_offset % 4); -- 4-7
                mood_val := 3;
                emotion_val := CASE 
                    WHEN day_offset % 3 = 0 THEN 'okay'
                    WHEN day_offset % 3 = 1 THEN 'calm'
                    ELSE 'anxious'
                END;
            ELSIF student_rec.first_name = 'Olivia' THEN
                -- Olivia: Improving
                stress_val := 6 - (day_offset / 2); -- 6 down to 3
                mood_val := 3 + (day_offset / 2); -- 3 up to 5
                emotion_val := CASE 
                    WHEN day_offset <= 2 THEN 'anxious'
                    WHEN day_offset <= 4 THEN 'okay'
                    ELSE 'calm'
                END;
            ELSE
                -- Default pattern for others
                stress_val := 5 + (day_offset % 3); -- 5-7
                mood_val := 3;
                emotion_val := CASE 
                    WHEN day_offset % 2 = 0 THEN 'okay'
                    ELSE 'anxious'
                END;
            END IF;
            
            -- Ensure values are in valid ranges
            stress_val := GREATEST(1, LEAST(10, stress_val));
            mood_val := GREATEST(1, LEAST(5, mood_val));
            
            -- Calculate derived values
            heart_rate_val := 60 + (stress_val * 3) + (day_offset * 2); -- 60-90
            water_val := 1500 + (day_offset * 100); -- 1500-2100 mL
            steps_val := 5000 + (day_offset * 500) + (RANDOM() * 2000)::int; -- 5000-8500
            sleep_val := 7.0 + (RANDOM() * 2.0); -- 7-9 hours
            wellness_val := 50 + (5 - stress_val) * 5 + (mood_val - 2) * 5; -- 50-90
            
            -- Insert daily check-in
            INSERT INTO daily_checkins (student_id, date, stress_level, mood_rating, emotion, emotion_intensity, stress_source, additional_notes)
            VALUES (
                student_rec.id,
                CURRENT_DATE - day_offset,
                stress_val,
                mood_val,
                emotion_val,
                stress_val,
                CASE 
                    WHEN stress_val >= 7 THEN 'School work'
                    WHEN stress_val >= 5 THEN 'Social relationships'
                    ELSE NULL
                END,
                CASE 
                    WHEN day_offset = 0 THEN 'Had a ' || emotion_val || ' day'
                    ELSE NULL
                END
            );
            
            -- Insert FHIR observations for mood (LOINC 39156-5)
            INSERT INTO fhir_observations (observation_id, student_id, loinc_code, loinc_display, value_string, effective_date_time, subject_reference)
            VALUES (
                'mood-' || student_rec.id || '-' || day_offset || '-' || EXTRACT(EPOCH FROM NOW())::bigint,
                student_rec.id,
                '39156-5',
                'Mood Assessment',
                emotion_val,
                CURRENT_TIMESTAMP - (day_offset * INTERVAL '1 day'),
                'Patient/' || student_rec.id
            );
            
            -- Insert FHIR observations for stress (LOINC 75258-2)
            INSERT INTO fhir_observations (observation_id, student_id, loinc_code, loinc_display, value_quantity, value_unit, effective_date_time, subject_reference)
            VALUES (
                'stress-' || student_rec.id || '-' || day_offset || '-' || EXTRACT(EPOCH FROM NOW())::bigint,
                student_rec.id,
                '75258-2',
                'Stress Level',
                stress_val::numeric,
                'score',
                CURRENT_TIMESTAMP - (day_offset * INTERVAL '1 day'),
                'Patient/' || student_rec.id
            );
            
            -- Insert FHIR observations for heart rate (LOINC 8867-4)
            INSERT INTO fhir_observations (observation_id, student_id, loinc_code, loinc_display, value_quantity, value_unit, effective_date_time, subject_reference)
            VALUES (
                'hr-' || student_rec.id || '-' || day_offset || '-' || EXTRACT(EPOCH FROM NOW())::bigint,
                student_rec.id,
                '8867-4',
                'Heart Rate',
                heart_rate_val::numeric,
                'BPM',
                CURRENT_TIMESTAMP - (day_offset * INTERVAL '1 day'),
                'Patient/' || student_rec.id
            );
            
            -- Insert activity log
            INSERT INTO activity_logs (student_id, date, steps, sleep_hours, hydration_percent, nutrition_percent)
            VALUES (
                student_rec.id,
                CURRENT_DATE - day_offset,
                steps_val,
                sleep_val,
                50 + (water_val / 40)::int, -- hydration percent
                60 + (RANDOM() * 20)::int -- nutrition percent
            );
            
            -- Insert wearable data
            INSERT INTO wearable_data (student_id, date, heart_rate, temperature, steps, sleep_hours)
            VALUES (
                student_rec.id,
                CURRENT_DATE - day_offset,
                heart_rate_val,
                36.5 + (RANDOM() * 1.0), -- temperature
                steps_val,
                sleep_val
            );
            
            -- Insert wellness score
            INSERT INTO wellness_scores (student_id, score, date, sentiment)
            VALUES (
                student_rec.id,
                wellness_val,
                CURRENT_DATE - day_offset,
                CASE 
                    WHEN wellness_val >= 70 THEN 'positive'
                    WHEN wellness_val >= 50 THEN 'neutral'
                    ELSE 'negative'
                END
            );
        END LOOP;
    END LOOP;
END $$;

COMMIT;

-- Verify data
SELECT 
    'Check-ins (past 7 days):' as type, 
    COUNT(*)::text as count 
FROM daily_checkins 
WHERE date >= CURRENT_DATE - INTERVAL '7 days'
UNION ALL
SELECT 
    'FHIR Observations (past 7 days):', 
    COUNT(*)::text 
FROM fhir_observations 
WHERE effective_date_time >= CURRENT_TIMESTAMP - INTERVAL '7 days'
UNION ALL
SELECT 
    'Activity Logs (past 7 days):', 
    COUNT(*)::text 
FROM activity_logs 
WHERE date >= CURRENT_DATE - INTERVAL '7 days'
UNION ALL
SELECT 
    'Wellness Scores (past 7 days):', 
    COUNT(*)::text 
FROM wellness_scores 
WHERE date >= CURRENT_DATE - INTERVAL '7 days';

