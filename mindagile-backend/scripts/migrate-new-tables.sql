-- Migration script to add missing tables for enhanced features
-- Updated to match Mindaigle database structure
-- Note: Reminder fields are now embedded in students table, not a separate table
-- Run this script on your database to add the new tables

-- Emergency Alerts Table (matches Mindaigle structure - no location field, has updated_at)
CREATE TABLE IF NOT EXISTS emergency_alerts (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE NOT NULL,
    alert_type VARCHAR(50) DEFAULT 'emergency' CHECK (alert_type IN ('emergency', 'urgent', 'support')),
    status VARCHAR(50) DEFAULT 'active' CHECK (status IN ('active', 'acknowledged', 'resolved', 'cancelled')),
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    acknowledged_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
    acknowledged_at TIMESTAMP,
    resolved_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
    resolved_at TIMESTAMP,
    resolution_notes TEXT
);

-- Assistance Request Students Junction Table (for multiple child selection - matches Mindaigle)
CREATE TABLE IF NOT EXISTS assistance_request_students (
    id SERIAL PRIMARY KEY,
    assistance_request_id INTEGER REFERENCES assistance_requests(id) ON DELETE CASCADE NOT NULL,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(assistance_request_id, student_id)
);

-- Audit Log Table (resource_type is VARCHAR(100) per Mindaigle)
CREATE TABLE IF NOT EXISTS audit_logs (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    user_role VARCHAR(50),
    action_type VARCHAR(100) NOT NULL, -- 'alert_created', 'reminder_sent', 'request_created', etc.
    resource_type VARCHAR(100), -- 'alert', 'reminder', 'request', 'checkin', etc. (VARCHAR(100) per Mindaigle)
    resource_id INTEGER,
    details JSONB, -- Additional context
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Schools Table (if not exists)
CREATE TABLE IF NOT EXISTS schools (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    address TEXT,
    phone VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Parents Table (if not exists)
CREATE TABLE IF NOT EXISTS parents (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    profile_pic TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Associates Table (if not exists)
CREATE TABLE IF NOT EXISTS associates (
    id SERIAL PRIMARY KEY,
    associate_id VARCHAR(255) UNIQUE NOT NULL,
    school_id INTEGER REFERENCES schools(id) ON DELETE SET NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(50),
    email VARCHAR(255) UNIQUE NOT NULL,
    address TEXT,
    profile_pic TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Associates-Schools Junction Table (if not exists)
CREATE TABLE IF NOT EXISTS associates_schools (
    id SERIAL PRIMARY KEY,
    associate_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    school_id INTEGER REFERENCES schools(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(associate_id, school_id)
);

-- Appointments Table (if not exists)
CREATE TABLE IF NOT EXISTS appointments (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE NOT NULL,
    staff_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    parent_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    appointment_date TIMESTAMP NOT NULL,
    duration INTEGER DEFAULT 30,
    type VARCHAR(100) NOT NULL DEFAULT 'general',
    notes TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'confirmed', 'completed', 'cancelled')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by INTEGER REFERENCES users(id) ON DELETE SET NULL
);

-- Activity Timeline Table (if not exists)
CREATE TABLE IF NOT EXISTS activity_timeline (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    actor_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    action_type VARCHAR(100) NOT NULL,
    summary TEXT NOT NULL,
    details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Counselor Notes Table (if not exists)
CREATE TABLE IF NOT EXISTS counselor_notes (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    counselor_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    note_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Interventions Table (if not exists)
CREATE TABLE IF NOT EXISTS interventions (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    counselor_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    intervention_type VARCHAR(100) NOT NULL,
    description TEXT,
    outcome TEXT,
    status VARCHAR(50) DEFAULT 'active' CHECK (status IN ('active', 'completed', 'discontinued')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add reminder fields to students table if they don't exist (embedded per Mindaigle structure)
DO $$
BEGIN
    -- Add reminder_enabled if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'students' AND column_name = 'reminder_enabled') THEN
        ALTER TABLE students ADD COLUMN reminder_enabled BOOLEAN DEFAULT true;
    END IF;

    -- Add reminder_interval_hours if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'students' AND column_name = 'reminder_interval_hours') THEN
        ALTER TABLE students ADD COLUMN reminder_interval_hours INTEGER DEFAULT 24 
            CHECK (reminder_interval_hours > 0);
    END IF;

    -- Add last_missed_checkin_at if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'students' AND column_name = 'last_missed_checkin_at') THEN
        ALTER TABLE students ADD COLUMN last_missed_checkin_at TIMESTAMP;
    END IF;

    -- Add last_reminder_sent_at if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'students' AND column_name = 'last_reminder_sent_at') THEN
        ALTER TABLE students ADD COLUMN last_reminder_sent_at TIMESTAMP;
    END IF;

    -- Add reminder_config_updated_at if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'students' AND column_name = 'reminder_config_updated_at') THEN
        ALTER TABLE students ADD COLUMN reminder_config_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
    END IF;
END $$;

-- Add request_type to assistance_requests if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'assistance_requests' AND column_name = 'request_type') THEN
        ALTER TABLE assistance_requests ADD COLUMN request_type VARCHAR(50) DEFAULT 'single' 
            CHECK (request_type IN ('single', 'multiple', 'all'));
    END IF;
END $$;

-- Add updated_at to emergency_alerts if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'emergency_alerts' AND column_name = 'updated_at') THEN
        ALTER TABLE emergency_alerts ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
    END IF;
END $$;

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_emergency_alerts_student ON emergency_alerts(student_id);
CREATE INDEX IF NOT EXISTS idx_emergency_alerts_status ON emergency_alerts(status);
CREATE INDEX IF NOT EXISTS idx_emergency_alerts_created ON emergency_alerts(created_at);
CREATE INDEX IF NOT EXISTS idx_assistance_request_students_request ON assistance_request_students(assistance_request_id);
CREATE INDEX IF NOT EXISTS idx_assistance_request_students_student ON assistance_request_students(student_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action_type);
CREATE INDEX IF NOT EXISTS idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_appointments_student ON appointments(student_id);
CREATE INDEX IF NOT EXISTS idx_appointments_staff ON appointments(staff_id);
CREATE INDEX IF NOT EXISTS idx_appointments_date ON appointments(appointment_date);
CREATE INDEX IF NOT EXISTS idx_activity_timeline_student ON activity_timeline(student_id);
CREATE INDEX IF NOT EXISTS idx_counselor_notes_student ON counselor_notes(student_id);
CREATE INDEX IF NOT EXISTS idx_interventions_student ON interventions(student_id);

-- Function to update updated_at timestamp (if not exists)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for updated_at
DROP TRIGGER IF EXISTS update_emergency_alerts_updated_at ON emergency_alerts;
CREATE TRIGGER update_emergency_alerts_updated_at BEFORE UPDATE ON emergency_alerts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_appointments_updated_at ON appointments;
CREATE TRIGGER update_appointments_updated_at BEFORE UPDATE ON appointments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_counselor_notes_updated_at ON counselor_notes;
CREATE TRIGGER update_counselor_notes_updated_at BEFORE UPDATE ON counselor_notes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_interventions_updated_at ON interventions;
CREATE TRIGGER update_interventions_updated_at BEFORE UPDATE ON interventions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_parents_updated_at ON parents;
CREATE TRIGGER update_parents_updated_at BEFORE UPDATE ON parents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_associates_updated_at ON associates;
CREATE TRIGGER update_associates_updated_at BEFORE UPDATE ON associates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Success message
DO $$
BEGIN
    RAISE NOTICE 'Migration completed successfully! All new tables have been created and columns added.';
END $$;
