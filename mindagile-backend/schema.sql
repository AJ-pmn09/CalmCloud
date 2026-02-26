-- MindAigle Extended Schema for K-12 Mental Health Platform
-- Based on Midnaigle schema with additions for FHIR, assistance requests, and role-based access
-- Updated to match Mindaigle database structure from database_recreation.json

-- ============================================
-- CORE TABLES
-- ============================================

-- Users table (matches Mindaigle structure)
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('student', 'parent', 'associate', 'expert', 'staff', 'admin')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    profile_picture_url TEXT,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(50)
);

-- Schools table
CREATE TABLE IF NOT EXISTS schools (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    address TEXT,
    phone VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Students table (with reminder fields embedded - matches Mindaigle structure)
CREATE TABLE IF NOT EXISTS students (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    grade INTEGER,
    school_id INTEGER REFERENCES schools(id) ON DELETE SET NULL,
    profile_picture_url TEXT,
    location VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- Reminder configuration fields (embedded in students table per Mindaigle structure)
    reminder_enabled BOOLEAN DEFAULT true,
    reminder_interval_hours INTEGER DEFAULT 24 CHECK (reminder_interval_hours > 0),
    last_missed_checkin_at TIMESTAMP,
    last_reminder_sent_at TIMESTAMP,
    reminder_config_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Parents table
CREATE TABLE IF NOT EXISTS parents (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    profile_pic TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Associates table
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

-- Associates-Schools junction table
CREATE TABLE IF NOT EXISTS associates_schools (
    id SERIAL PRIMARY KEY,
    associate_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    school_id INTEGER REFERENCES schools(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(associate_id, school_id)
);

-- Daily checkins (extended for emotions)
CREATE TABLE IF NOT EXISTS daily_checkins (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    date DATE DEFAULT CURRENT_DATE,
    stress_level INTEGER CHECK (stress_level >= 1 AND stress_level <= 10),
    mood_rating INTEGER CHECK (mood_rating >= 1 AND mood_rating <= 5),
    emotion VARCHAR(50), -- happy, calm, okay, sad, anxious, stressed
    emotion_intensity INTEGER CHECK (emotion_intensity >= 1 AND emotion_intensity <= 10),
    stress_source VARCHAR(255),
    additional_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Activity logs (extended with additional fields from Mindaigle)
CREATE TABLE IF NOT EXISTS activity_logs (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    date DATE DEFAULT CURRENT_DATE,
    steps INTEGER DEFAULT 0,
    sleep_hours NUMERIC(4,2) DEFAULT 0,
    hydration_percent INTEGER DEFAULT 0,
    nutrition_percent INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- Additional fields from Mindaigle
    stress_level INTEGER,
    stress_source VARCHAR(255),
    heart_rate INTEGER,
    mood VARCHAR(50),
    notes TEXT
);

-- Wearable data
CREATE TABLE IF NOT EXISTS wearable_data (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    date DATE DEFAULT CURRENT_DATE,
    heart_rate INTEGER,
    temperature NUMERIC(4,2),
    steps INTEGER,
    sleep_hours NUMERIC(4,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Wellness scores
CREATE TABLE IF NOT EXISTS wellness_scores (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    score INTEGER CHECK (score >= 0 AND score <= 100),
    date DATE DEFAULT CURRENT_DATE,
    sentiment VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Symptom logs
CREATE TABLE IF NOT EXISTS symptom_logs (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    date DATE DEFAULT CURRENT_DATE,
    symptom_type VARCHAR(100),
    severity INTEGER CHECK (severity >= 1 AND severity <= 5),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Resources
CREATE TABLE IF NOT EXISTS resources (
    id SERIAL PRIMARY KEY,
    category VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    address TEXT,
    phone VARCHAR(50),
    distance_km NUMERIC(6,2),
    latitude NUMERIC(10,8),
    longitude NUMERIC(11,8),
    status VARCHAR(50),
    operating_hours TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- K-12 MENTAL HEALTH PLATFORM TABLES
-- ============================================

-- Parent-Child Linking Table
CREATE TABLE IF NOT EXISTS parent_child_links (
    id SERIAL PRIMARY KEY,
    parent_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    student_email VARCHAR(255), -- For pending links before student signs up
    status VARCHAR(50) DEFAULT 'active' CHECK (status IN ('pending', 'active', 'inactive')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    linked_at TIMESTAMP,
    UNIQUE(parent_id, student_id),
    UNIQUE(parent_id, student_email)
);

-- Assistance Requests Table (with request_type field)
CREATE TABLE IF NOT EXISTS assistance_requests (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    parent_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    parent_name VARCHAR(255),
    message TEXT NOT NULL,
    urgency VARCHAR(50) NOT NULL CHECK (urgency IN ('low', 'normal', 'high')),
    status VARCHAR(50) DEFAULT 'pending' CHECK (status IN ('pending', 'in-progress', 'resolved')),
    handled_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
    handled_by_name VARCHAR(255),
    handled_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    request_type VARCHAR(50) DEFAULT 'single' CHECK (request_type IN ('single', 'multiple', 'all'))
);

-- Assistance Request Students Junction Table (for multiple child selection - matches Mindaigle)
CREATE TABLE IF NOT EXISTS assistance_request_students (
    id SERIAL PRIMARY KEY,
    assistance_request_id INTEGER REFERENCES assistance_requests(id) ON DELETE CASCADE NOT NULL,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(assistance_request_id, student_id)
);

-- FHIR Observations Table (for FHIR R4 compliance)
CREATE TABLE IF NOT EXISTS fhir_observations (
    id SERIAL PRIMARY KEY,
    observation_id VARCHAR(255) UNIQUE NOT NULL, -- FHIR resource ID
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    resource_type VARCHAR(50) DEFAULT 'Observation',
    status VARCHAR(50) DEFAULT 'final',
    loinc_code VARCHAR(50) NOT NULL, -- LOINC code (75258-2, 73985-4, etc.)
    loinc_display VARCHAR(255), -- Human-readable name
    value_quantity NUMERIC(10,2), -- For numeric values (stress, heart rate)
    value_string VARCHAR(255), -- For text values (emotion names)
    value_unit VARCHAR(50), -- Unit for quantity (BPM, mL, etc.)
    effective_date_time TIMESTAMP NOT NULL,
    subject_reference VARCHAR(255), -- Patient/{studentId}
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- FHIR Conditions Table
CREATE TABLE IF NOT EXISTS fhir_conditions (
    id SERIAL PRIMARY KEY,
    condition_id VARCHAR(255) UNIQUE NOT NULL,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    resource_type VARCHAR(50) DEFAULT 'Condition',
    clinical_status VARCHAR(50) DEFAULT 'active',
    code_system VARCHAR(255),
    code_code VARCHAR(255),
    code_display VARCHAR(255),
    subject_reference VARCHAR(255),
    recorded_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- FHIR Care Teams Table
CREATE TABLE IF NOT EXISTS fhir_care_teams (
    id SERIAL PRIMARY KEY,
    care_team_id VARCHAR(255) UNIQUE NOT NULL,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    resource_type VARCHAR(50) DEFAULT 'CareTeam',
    status VARCHAR(50) DEFAULT 'active',
    name VARCHAR(255),
    subject_reference VARCHAR(255),
    participant_data JSONB, -- Array of care team members
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Emergency Alerts Table (matches Mindaigle structure - no location field, has updated_at)
CREATE TABLE IF NOT EXISTS emergency_alerts (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE NOT NULL,
    alert_type VARCHAR(50) DEFAULT 'emergency' CHECK (alert_type IN ('emergency', 'urgent', 'support')),
    message TEXT,
    status VARCHAR(50) DEFAULT 'active' CHECK (status IN ('active', 'acknowledged', 'resolved', 'cancelled')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    acknowledged_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
    acknowledged_at TIMESTAMP,
    resolved_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
    resolved_at TIMESTAMP,
    resolution_notes TEXT
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

-- Appointments Table
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

-- Activity Timeline Table
CREATE TABLE IF NOT EXISTS activity_timeline (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    actor_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    action_type VARCHAR(100) NOT NULL,
    summary TEXT NOT NULL,
    details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Counselor Notes Table (note_text + note_json for NLP tasks)
CREATE TABLE IF NOT EXISTS counselor_notes (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    counselor_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    note_text TEXT NOT NULL,
    note_json JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Interventions Table
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

-- Clinical Screeners (PHQ-9 / GAD-7, COCM-style)
CREATE TABLE IF NOT EXISTS screener_instances (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE NOT NULL,
    screener_type VARCHAR(50) NOT NULL CHECK (screener_type IN ('phq9', 'gad7')),
    status VARCHAR(50) DEFAULT 'assigned' CHECK (status IN ('assigned', 'in_progress', 'completed', 'expired')),
    trigger_source VARCHAR(50) DEFAULT 'manual' CHECK (trigger_source IN ('manual', 'scheduled', 'trigger')),
    assigned_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS screener_responses (
    id SERIAL PRIMARY KEY,
    instance_id INTEGER REFERENCES screener_instances(id) ON DELETE CASCADE NOT NULL,
    question_index INTEGER NOT NULL CHECK (question_index >= 0),
    answer_value INTEGER NOT NULL CHECK (answer_value >= 0 AND answer_value <= 3),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(instance_id, question_index)
);

CREATE TABLE IF NOT EXISTS screener_scores (
    id SERIAL PRIMARY KEY,
    instance_id INTEGER REFERENCES screener_instances(id) ON DELETE CASCADE NOT NULL UNIQUE,
    total INTEGER NOT NULL CHECK (total >= 0),
    severity VARCHAR(50) NOT NULL,
    positive BOOLEAN NOT NULL DEFAULT false,
    details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notifications (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    related_type VARCHAR(100),
    related_id INTEGER,
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_students_user_id ON students(user_id);
CREATE INDEX IF NOT EXISTS idx_students_school_id ON students(school_id);
CREATE INDEX IF NOT EXISTS idx_daily_checkins_student_date ON daily_checkins(student_id, date);
CREATE INDEX IF NOT EXISTS idx_parent_child_parent ON parent_child_links(parent_id);
CREATE INDEX IF NOT EXISTS idx_parent_child_student ON parent_child_links(student_id);
CREATE INDEX IF NOT EXISTS idx_assistance_requests_status ON assistance_requests(status);
CREATE INDEX IF NOT EXISTS idx_assistance_requests_urgency ON assistance_requests(urgency);
CREATE INDEX IF NOT EXISTS idx_assistance_request_students_request ON assistance_request_students(assistance_request_id);
CREATE INDEX IF NOT EXISTS idx_assistance_request_students_student ON assistance_request_students(student_id);
CREATE INDEX IF NOT EXISTS idx_fhir_observations_student ON fhir_observations(student_id);
CREATE INDEX IF NOT EXISTS idx_fhir_observations_loinc ON fhir_observations(loinc_code);
CREATE INDEX IF NOT EXISTS idx_fhir_observations_date ON fhir_observations(effective_date_time);
CREATE INDEX IF NOT EXISTS idx_emergency_alerts_student ON emergency_alerts(student_id);
CREATE INDEX IF NOT EXISTS idx_emergency_alerts_status ON emergency_alerts(status);
CREATE INDEX IF NOT EXISTS idx_emergency_alerts_created ON emergency_alerts(created_at);
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
CREATE INDEX IF NOT EXISTS idx_screener_instances_student ON screener_instances(student_id);
CREATE INDEX IF NOT EXISTS idx_screener_instances_status ON screener_instances(status);
CREATE INDEX IF NOT EXISTS idx_screener_responses_instance ON screener_responses(instance_id);
CREATE INDEX IF NOT EXISTS idx_screener_scores_instance ON screener_scores(instance_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);

-- ============================================
-- FUNCTIONS AND TRIGGERS
-- ============================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for updated_at
CREATE TRIGGER update_assistance_requests_updated_at BEFORE UPDATE ON assistance_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_fhir_observations_updated_at BEFORE UPDATE ON fhir_observations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_emergency_alerts_updated_at BEFORE UPDATE ON emergency_alerts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_appointments_updated_at BEFORE UPDATE ON appointments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_counselor_notes_updated_at BEFORE UPDATE ON counselor_notes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_interventions_updated_at BEFORE UPDATE ON interventions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_parents_updated_at BEFORE UPDATE ON parents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_associates_updated_at BEFORE UPDATE ON associates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_screener_instances_updated_at BEFORE UPDATE ON screener_instances
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
