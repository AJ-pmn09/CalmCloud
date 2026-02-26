-- Create missing tables for MindAigle database
-- Run this on the VM to create all missing tables: emergency_alerts, communications, fhir_observations

-- FHIR Observations table (required for students endpoint)
CREATE TABLE IF NOT EXISTS fhir_observations (
    id SERIAL PRIMARY KEY,
    observation_id VARCHAR(255) UNIQUE NOT NULL,
    student_id INTEGER REFERENCES students(id) ON DELETE CASCADE,
    resource_type VARCHAR(50) DEFAULT 'Observation',
    status VARCHAR(50) DEFAULT 'final',
    loinc_code VARCHAR(50) NOT NULL,
    loinc_display VARCHAR(255),
    value_quantity NUMERIC(10,2),
    value_string VARCHAR(255),
    value_unit VARCHAR(50),
    effective_date_time TIMESTAMP NOT NULL,
    subject_reference VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for fhir_observations
CREATE INDEX IF NOT EXISTS idx_fhir_observations_student ON fhir_observations(student_id);
CREATE INDEX IF NOT EXISTS idx_fhir_observations_loinc ON fhir_observations(loinc_code);
CREATE INDEX IF NOT EXISTS idx_fhir_observations_date ON fhir_observations(effective_date_time);

-- Create trigger to update updated_at timestamp for fhir_observations
CREATE OR REPLACE FUNCTION update_fhir_observations_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_fhir_observations_updated_at ON fhir_observations;
CREATE TRIGGER update_fhir_observations_updated_at 
    BEFORE UPDATE ON fhir_observations
    FOR EACH ROW
    EXECUTE FUNCTION update_fhir_observations_updated_at();

-- Emergency Alerts table (matches schema.sql)
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

-- Create indexes for emergency_alerts
CREATE INDEX IF NOT EXISTS idx_emergency_alerts_student ON emergency_alerts(student_id);
CREATE INDEX IF NOT EXISTS idx_emergency_alerts_status ON emergency_alerts(status);
CREATE INDEX IF NOT EXISTS idx_emergency_alerts_created ON emergency_alerts(created_at);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_emergency_alerts_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_emergency_alerts_updated_at ON emergency_alerts;
CREATE TRIGGER update_emergency_alerts_updated_at 
    BEFORE UPDATE ON emergency_alerts
    FOR EACH ROW
    EXECUTE FUNCTION update_emergency_alerts_updated_at();

-- Communications table (if not exists)
CREATE TABLE IF NOT EXISTS communications (
    id SERIAL PRIMARY KEY,
    sender_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    recipient_type VARCHAR(50) NOT NULL CHECK (recipient_type IN ('student', 'cohort', 'all')),
    recipient_id INTEGER, -- student_id if recipient_type = 'student'
    recipient_cohort VARCHAR(100), -- cohort name if recipient_type = 'cohort'
    message TEXT NOT NULL,
    subject VARCHAR(255),
    priority VARCHAR(20) DEFAULT 'normal' CHECK (priority IN ('low', 'normal', 'high', 'urgent')),
    status VARCHAR(20) DEFAULT 'sent' CHECK (status IN ('draft', 'sent', 'read', 'archived')),
    emergency_override BOOLEAN DEFAULT false,
    parent_visible BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for communications
CREATE INDEX IF NOT EXISTS idx_communications_recipient ON communications(recipient_type, recipient_id, recipient_cohort);
CREATE INDEX IF NOT EXISTS idx_communications_sender ON communications(sender_id, created_at);
CREATE INDEX IF NOT EXISTS idx_communications_status ON communications(status, created_at);

-- Create trigger to update updated_at timestamp for communications
CREATE OR REPLACE FUNCTION update_communications_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_communications_updated_at ON communications;
CREATE TRIGGER update_communications_updated_at 
    BEFORE UPDATE ON communications
    FOR EACH ROW
    EXECUTE FUNCTION update_communications_updated_at();

-- Verify tables were created
SELECT 'emergency_alerts table exists' AS status WHERE EXISTS (
    SELECT FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_name = 'emergency_alerts'
);

SELECT 'communications table exists' AS status WHERE EXISTS (
    SELECT FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_name = 'communications'
);
