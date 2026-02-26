-- Migration script to add first_name and last_name columns to users table
-- and migrate data from name column

-- Step 1: Add new columns
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS first_name VARCHAR(100),
ADD COLUMN IF NOT EXISTS last_name VARCHAR(100),
ADD COLUMN IF NOT EXISTS phone VARCHAR(50);

-- Step 2: Migrate existing name data to first_name and last_name
-- Split name on first space: first part -> first_name, rest -> last_name
UPDATE users 
SET 
  first_name = CASE 
    WHEN name IS NULL OR name = '' THEN NULL
    WHEN position(' ' in name) > 0 THEN 
      substring(name from 1 for position(' ' in name) - 1)
    ELSE name
  END,
  last_name = CASE 
    WHEN name IS NULL OR name = '' THEN NULL
    WHEN position(' ' in name) > 0 THEN 
      substring(name from position(' ' in name) + 1)
    ELSE NULL
  END
WHERE first_name IS NULL AND name IS NOT NULL;

-- Step 3: For users without a name, use email prefix as first_name
UPDATE users 
SET first_name = split_part(email, '@', 1)
WHERE first_name IS NULL AND email IS NOT NULL;

-- Step 4: (Optional) Keep name column for backward compatibility
-- We'll keep it for now but code will use first_name/last_name

-- Verify migration
SELECT id, email, name, first_name, last_name FROM users LIMIT 5;

