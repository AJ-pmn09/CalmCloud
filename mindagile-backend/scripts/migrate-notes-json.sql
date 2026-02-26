-- Store notes in JSON format for NLP tasks (staff, children, parents)
-- Run per-school database.

-- Counselor notes: add note_json for structured NLP input
ALTER TABLE counselor_notes ADD COLUMN IF NOT EXISTS note_json JSONB;

-- Communications: add message_json for structured NLP input
ALTER TABLE communications ADD COLUMN IF NOT EXISTS message_json JSONB;

-- Daily check-ins (student notes): add additional_notes_json for NLP
ALTER TABLE daily_checkins ADD COLUMN IF NOT EXISTS additional_notes_json JSONB;

-- Assistance requests: add notes_json for parent/staff notes
ALTER TABLE assistance_requests ADD COLUMN IF NOT EXISTS notes_json JSONB;

-- Appointments: add notes_json for staff/parent notes
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS notes_json JSONB;

-- Emergency alerts resolution notes: add resolution_notes_json for NLP
ALTER TABLE emergency_alerts ADD COLUMN IF NOT EXISTS resolution_notes_json JSONB;

COMMENT ON COLUMN counselor_notes.note_json IS 'Structured JSON for NLP: { text, authorId, authorRole, createdAt, source }';
COMMENT ON COLUMN communications.message_json IS 'Structured JSON for NLP: { text, senderId, senderRole, recipientType, createdAt }';
COMMENT ON COLUMN daily_checkins.additional_notes_json IS 'Structured JSON for NLP: { text, studentId, date, source }';
