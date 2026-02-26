-- Add "mood" column to daily_checkins so backend studentData routes work (GET /student-data/me, /trends, GET /students).
-- Run this on the DB that the backend uses (e.g. mindaigle on port 5432 when using single-DB).

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'daily_checkins' AND column_name = 'mood'
  ) THEN
    ALTER TABLE daily_checkins ADD COLUMN mood INTEGER;
    COMMENT ON COLUMN daily_checkins.mood IS 'Mood value (integer) for API compatibility; mood_rating is primary.';
  END IF;
END $$;
