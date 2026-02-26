-- PHQ-9 / GAD-7 Clinical Screeners (COCM-style)
-- Catalog → Assignment (instance) → Student answers → Scoring → Staff notifications → Reporting

-- ============================================
-- SCREENER INSTANCES
-- ============================================
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

CREATE INDEX IF NOT EXISTS idx_screener_instances_student ON screener_instances(student_id);
CREATE INDEX IF NOT EXISTS idx_screener_instances_status ON screener_instances(status);
CREATE INDEX IF NOT EXISTS idx_screener_instances_type ON screener_instances(screener_type);
CREATE INDEX IF NOT EXISTS idx_screener_instances_completed ON screener_instances(completed_at) WHERE completed_at IS NOT NULL;

-- ============================================
-- SCREENER RESPONSES (one row per question)
-- ============================================
CREATE TABLE IF NOT EXISTS screener_responses (
    id SERIAL PRIMARY KEY,
    instance_id INTEGER REFERENCES screener_instances(id) ON DELETE CASCADE NOT NULL,
    question_index INTEGER NOT NULL CHECK (question_index >= 0),
    answer_value INTEGER NOT NULL CHECK (answer_value >= 0 AND answer_value <= 3),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(instance_id, question_index)
);

CREATE INDEX IF NOT EXISTS idx_screener_responses_instance ON screener_responses(instance_id);

-- ============================================
-- SCREENER SCORES (one row per completed instance)
-- ============================================
CREATE TABLE IF NOT EXISTS screener_scores (
    id SERIAL PRIMARY KEY,
    instance_id INTEGER REFERENCES screener_instances(id) ON DELETE CASCADE NOT NULL UNIQUE,
    total INTEGER NOT NULL CHECK (total >= 0),
    severity VARCHAR(50) NOT NULL,
    positive BOOLEAN NOT NULL DEFAULT false,
    details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_screener_scores_instance ON screener_scores(instance_id);
CREATE INDEX IF NOT EXISTS idx_screener_scores_positive ON screener_scores(positive) WHERE positive = true;

-- ============================================
-- NOTIFICATIONS (for positive screen → notify all staff)
-- ============================================
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

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read ON notifications(read_at) WHERE read_at IS NULL;

COMMENT ON TABLE screener_instances IS 'PHQ-9/GAD-7 assignments to students; 2-week frequency enforced in API';
COMMENT ON TABLE screener_responses IS 'Per-question answers (0-3) for each instance';
COMMENT ON TABLE screener_scores IS 'Computed total, severity, positive flag per completed instance';
COMMENT ON TABLE notifications IS 'In-app notifications e.g. positive screener alert for staff';
