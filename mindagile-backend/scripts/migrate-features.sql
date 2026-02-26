-- Migration script for new features:
-- 1. Missed Check-In Reminder System
-- 2. Selective Communication Feature (already exists, but adding indexes)
-- 3. Student Emergency Contact System with Suicide-Risk Screening

-- ============================================
-- REMINDER LOGS TABLE
-- ============================================
-- Track all reminder events for audit and analytics
CREATE TABLE IF NOT EXISTS reminder_logs (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE NOT NULL,
    reminder_type VARCHAR(50) DEFAULT 'missed_checkin' CHECK (reminder_type IN ('missed_checkin', 'scheduled', 'escalated')),
    escalation_level VARCHAR(50) DEFAULT 'normal' CHECK (escalation_level IN ('normal', 'escalated', 'critical')),
    reminder_interval_hours INTEGER NOT NULL,
    days_since_checkin INTEGER,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notification_method VARCHAR(50) DEFAULT 'app' CHECK (notification_method IN ('app', 'email', 'push', 'sms')),
    status VARCHAR(50) DEFAULT 'sent' CHECK (status IN ('sent', 'delivered', 'failed', 'cancelled')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reminder_logs_student ON reminder_logs(student_id, sent_at);
CREATE INDEX IF NOT EXISTS idx_reminder_logs_escalation ON reminder_logs(escalation_level, sent_at);

-- ============================================
-- SUICIDE-RISK SCREENING TABLE
-- ============================================
-- Store suicide-risk screening responses when emergency alert is triggered
CREATE TABLE IF NOT EXISTS suicide_risk_screenings (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE NOT NULL,
    emergency_alert_id INTEGER REFERENCES emergency_alerts(id) ON DELETE SET NULL,
    screening_questions JSONB NOT NULL, -- Store questions and responses
    risk_score INTEGER CHECK (risk_score >= 0 AND risk_score <= 10), -- 0-10 scale
    risk_level VARCHAR(50) DEFAULT 'low' CHECK (risk_level IN ('low', 'moderate', 'high', 'critical')),
    immediate_action_required BOOLEAN DEFAULT false,
    screening_completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_suicide_risk_screenings_student ON suicide_risk_screenings(student_id, screening_completed_at);
CREATE INDEX IF NOT EXISTS idx_suicide_risk_screenings_alert ON suicide_risk_screenings(emergency_alert_id);
CREATE INDEX IF NOT EXISTS idx_suicide_risk_screenings_risk ON suicide_risk_screenings(risk_level, screening_completed_at);

-- ============================================
-- ENHANCE COMMUNICATIONS TABLE INDEXES
-- ============================================
-- Add indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_communications_status ON communications(status, created_at);
CREATE INDEX IF NOT EXISTS idx_communications_priority ON communications(priority, created_at);
CREATE INDEX IF NOT EXISTS idx_communications_emergency ON communications(emergency_override, created_at) WHERE emergency_override = true;

-- ============================================
-- ENHANCE STUDENTS TABLE
-- ============================================
-- Add smart scheduling fields to avoid notification fatigue
ALTER TABLE students 
ADD COLUMN IF NOT EXISTS reminder_smart_scheduling BOOLEAN DEFAULT true,
ADD COLUMN IF NOT EXISTS reminder_quiet_hours_start TIME DEFAULT '22:00:00',
ADD COLUMN IF NOT EXISTS reminder_quiet_hours_end TIME DEFAULT '07:00:00',
ADD COLUMN IF NOT EXISTS reminder_max_per_day INTEGER DEFAULT 3,
ADD COLUMN IF NOT EXISTS reminder_count_today INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS reminder_last_reset_date DATE DEFAULT CURRENT_DATE;

-- ============================================
-- COMMENTS
-- ============================================
COMMENT ON TABLE reminder_logs IS 'Tracks all reminder events for missed check-ins with escalation levels';
COMMENT ON TABLE suicide_risk_screenings IS 'Stores suicide-risk screening responses when students trigger emergency alerts';
COMMENT ON COLUMN students.reminder_smart_scheduling IS 'Enable smart scheduling to avoid notification fatigue';
COMMENT ON COLUMN students.reminder_quiet_hours_start IS 'Start time for quiet hours (no reminders sent)';
COMMENT ON COLUMN students.reminder_quiet_hours_end IS 'End time for quiet hours (no reminders sent)';
COMMENT ON COLUMN students.reminder_max_per_day IS 'Maximum reminders per day to avoid fatigue';
COMMENT ON COLUMN students.reminder_count_today IS 'Count of reminders sent today (reset daily)';

