-- Create emergency_alerts table in all school databases
-- Run this script on each school database (Horizons, Houghton, Calumet)

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

-- Create trigger to update updated_at timestamp for emergency_alerts
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

-- Verify table was created
SELECT 'emergency_alerts table created successfully' AS status;
