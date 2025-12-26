-- CalmCloud Extended Schema for K-12 Mental Health Platform
-- Based on Midnaigle schema with additions for FHIR, assistance requests, and role-based access

-- ============================================
-- EXISTING TABLES (from Midnaigle)
-- ============================================

-- Users table (extended for new roles)
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    role VARCHAR(50) NOT NULL CHECK (role IN ('student', 'parent', 'associate', 'expert', 'staff', 'admin')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    profile_picture_url TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Students table
CREATE TABLE IF NOT EXISTS students (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    grade INTEGER,
    school_id INTEGER,
    profile_picture_url TEXT,
    location VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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

-- Activity logs
CREATE TABLE IF NOT EXISTS activity_logs (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    date DATE DEFAULT CURRENT_DATE,
    steps INTEGER DEFAULT 0,
    sleep_hours NUMERIC(4,2) DEFAULT 0,
    hydration_percent INTEGER DEFAULT 0,
    nutrition_percent INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
-- NEW TABLES FOR K-12 MENTAL HEALTH PLATFORM
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

-- Assistance Requests Table
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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_students_user_id ON students(user_id);
CREATE INDEX IF NOT EXISTS idx_daily_checkins_student_date ON daily_checkins(student_id, date);
CREATE INDEX IF NOT EXISTS idx_parent_child_parent ON parent_child_links(parent_id);
CREATE INDEX IF NOT EXISTS idx_parent_child_student ON parent_child_links(student_id);
CREATE INDEX IF NOT EXISTS idx_assistance_requests_status ON assistance_requests(status);
CREATE INDEX IF NOT EXISTS idx_assistance_requests_urgency ON assistance_requests(urgency);
CREATE INDEX IF NOT EXISTS idx_fhir_observations_student ON fhir_observations(student_id);
CREATE INDEX IF NOT EXISTS idx_fhir_observations_loinc ON fhir_observations(loinc_code);
CREATE INDEX IF NOT EXISTS idx_fhir_observations_date ON fhir_observations(effective_date_time);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_assistance_requests_updated_at BEFORE UPDATE ON assistance_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_fhir_observations_updated_at BEFORE UPDATE ON fhir_observations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

