-- Setup Sample Data Script
-- This script creates the sample data structure as shown in the terminal output
-- It will clean existing test data and create only the required sample data

BEGIN;

-- Step 1: Create missing tables if they don't exist
CREATE TABLE IF NOT EXISTS schools (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    address TEXT,
    phone VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS associates_schools (
    id SERIAL PRIMARY KEY,
    associate_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    school_id INTEGER REFERENCES schools(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(associate_id, school_id)
);

-- Step 2: Clean up old test data
-- Delete old test users (IDs 1-4) and their related data
DELETE FROM students WHERE user_id IN (SELECT id FROM users WHERE id IN (1, 2, 3, 4));
DELETE FROM parent_child_links WHERE parent_id IN (SELECT id FROM users WHERE id IN (1, 2, 3, 4));
DELETE FROM assistance_requests WHERE parent_id IN (SELECT id FROM users WHERE id IN (1, 2, 3, 4));
DELETE FROM users WHERE id IN (1, 2, 3, 4);

-- Step 3: Create Schools
INSERT INTO schools (id, name, address, phone) VALUES
(7, 'Horizon High School', '123 Education Ave, Horizon City', '555-0100'),
(8, 'Houghton High School', '456 Learning Lane, Houghton', '555-0200'),
(9, 'Calumet Middle School', '789 Knowledge St, Calumet', '555-0300')
ON CONFLICT (id) DO UPDATE SET 
    name = EXCLUDED.name,
    address = EXCLUDED.address,
    phone = EXCLUDED.phone;

-- Reset sequence for schools
SELECT setval('schools_id_seq', 9, true);

-- Step 4: Create/Update Students (12 students)
-- Use INSERT ... ON CONFLICT to update existing or insert new
INSERT INTO users (email, password_hash, first_name, last_name, role) VALUES
('emma.thompson@horizon.edu', '$2b$10$dummyhash', 'Emma', 'Thompson', 'student'),
('james.martinez@horizon.edu', '$2b$10$dummyhash', 'James', 'Martinez', 'student'),
('sophia.chen@horizon.edu', '$2b$10$dummyhash', 'Sophia', 'Chen', 'student'),
('michael.anderson@horizon.edu', '$2b$10$dummyhash', 'Michael', 'Anderson', 'student'),
('olivia.williams@houghton.edu', '$2b$10$dummyhash', 'Olivia', 'Williams', 'student'),
('noah.brown@houghton.edu', '$2b$10$dummyhash', 'Noah', 'Brown', 'student'),
('ava.davis@houghton.edu', '$2b$10$dummyhash', 'Ava', 'Davis', 'student'),
('liam.garcia@houghton.edu', '$2b$10$dummyhash', 'Liam', 'Garcia', 'student'),
('isabella.rodriguez@calumet.edu', '$2b$10$dummyhash', 'Isabella', 'Rodriguez', 'student'),
('ethan.wilson@calumet.edu', '$2b$10$dummyhash', 'Ethan', 'Wilson', 'student'),
('mia.moore@calumet.edu', '$2b$10$dummyhash', 'Mia', 'Moore', 'student'),
('lucas.taylor@calumet.edu', '$2b$10$dummyhash', 'Lucas', 'Taylor', 'student')
ON CONFLICT (email) DO UPDATE SET 
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    role = EXCLUDED.role;

-- Get user IDs and create/update student records
-- First delete existing student records for these users
DELETE FROM students WHERE user_id IN (
    SELECT id FROM users WHERE role = 'student' 
      AND email IN (
        'emma.thompson@horizon.edu', 'james.martinez@horizon.edu', 'sophia.chen@horizon.edu',
        'michael.anderson@horizon.edu', 'olivia.williams@houghton.edu', 'noah.brown@houghton.edu',
        'ava.davis@houghton.edu', 'liam.garcia@houghton.edu', 'isabella.rodriguez@calumet.edu',
        'ethan.wilson@calumet.edu', 'mia.moore@calumet.edu', 'lucas.taylor@calumet.edu'
      )
);

-- Then insert new student records
INSERT INTO students (user_id, first_name, last_name, school_id, grade)
SELECT u.id, u.first_name, u.last_name, 
    CASE 
        WHEN u.email LIKE '%@horizon.edu' THEN 7
        WHEN u.email LIKE '%@houghton.edu' THEN 8
        WHEN u.email LIKE '%@calumet.edu' THEN 9
    END,
    CASE 
        WHEN u.email IN ('emma.thompson@horizon.edu', 'sophia.chen@horizon.edu') THEN 10
        WHEN u.email = 'james.martinez@horizon.edu' THEN 11
        WHEN u.email = 'michael.anderson@horizon.edu' THEN 12
        WHEN u.email IN ('olivia.williams@houghton.edu', 'ava.davis@houghton.edu') THEN 9
        WHEN u.email = 'noah.brown@houghton.edu' THEN 10
        WHEN u.email = 'liam.garcia@houghton.edu' THEN 11
        WHEN u.email IN ('isabella.rodriguez@calumet.edu', 'mia.moore@calumet.edu') THEN 7
        WHEN u.email IN ('ethan.wilson@calumet.edu', 'lucas.taylor@calumet.edu') THEN 8
    END
FROM users u
WHERE u.role = 'student' 
  AND u.email IN (
    'emma.thompson@horizon.edu', 'james.martinez@horizon.edu', 'sophia.chen@horizon.edu',
    'michael.anderson@horizon.edu', 'olivia.williams@houghton.edu', 'noah.brown@houghton.edu',
    'ava.davis@houghton.edu', 'liam.garcia@houghton.edu', 'isabella.rodriguez@calumet.edu',
    'ethan.wilson@calumet.edu', 'mia.moore@calumet.edu', 'lucas.taylor@calumet.edu'
  );

-- Step 5: Create Parents (10 parents)
INSERT INTO users (email, password_hash, first_name, last_name, role) VALUES
('sarah.thompson@email.com', '$2b$10$dummyhash', 'Sarah', 'Thompson', 'parent'),
('david.wilson@email.com', '$2b$10$dummyhash', 'David', 'Wilson', 'parent'),
('maria.martinez@email.com', '$2b$10$dummyhash', 'Maria', 'Martinez', 'parent'),
('wei.chen@email.com', '$2b$10$dummyhash', 'Wei', 'Chen', 'parent'),
('robert.anderson@email.com', '$2b$10$dummyhash', 'Robert', 'Anderson', 'parent'),
('jennifer.brown@email.com', '$2b$10$dummyhash', 'Jennifer', 'Brown', 'parent'),
('christopher.davis@email.com', '$2b$10$dummyhash', 'Christopher', 'Davis', 'parent'),
('patricia.garcia@email.com', '$2b$10$dummyhash', 'Patricia', 'Garcia', 'parent'),
('carlos.rodriguez@email.com', '$2b$10$dummyhash', 'Carlos', 'Rodriguez', 'parent'),
('amanda.taylor@email.com', '$2b$10$dummyhash', 'Amanda', 'Taylor', 'parent')
ON CONFLICT (email) DO UPDATE SET
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    role = EXCLUDED.role;

-- Create parent-child links
-- First delete existing links
DELETE FROM parent_child_links WHERE parent_id IN (
    SELECT id FROM users WHERE role = 'parent' 
      AND email IN (
        'sarah.thompson@email.com', 'david.wilson@email.com', 'maria.martinez@email.com',
        'wei.chen@email.com', 'robert.anderson@email.com', 'jennifer.brown@email.com',
        'christopher.davis@email.com', 'patricia.garcia@email.com', 'carlos.rodriguez@email.com',
        'amanda.taylor@email.com'
      )
);

INSERT INTO parent_child_links (parent_id, student_id, status)
SELECT p.id, st.id, 'active'
FROM users p
JOIN users s ON (
    (p.email = 'sarah.thompson@email.com' AND s.email IN ('emma.thompson@horizon.edu', 'olivia.williams@houghton.edu')) OR
    (p.email = 'david.wilson@email.com' AND s.email IN ('ethan.wilson@calumet.edu', 'mia.moore@calumet.edu')) OR
    (p.email = 'maria.martinez@email.com' AND s.email = 'james.martinez@horizon.edu') OR
    (p.email = 'wei.chen@email.com' AND s.email = 'sophia.chen@horizon.edu') OR
    (p.email = 'robert.anderson@email.com' AND s.email = 'michael.anderson@horizon.edu') OR
    (p.email = 'jennifer.brown@email.com' AND s.email = 'noah.brown@houghton.edu') OR
    (p.email = 'christopher.davis@email.com' AND s.email = 'ava.davis@houghton.edu') OR
    (p.email = 'patricia.garcia@email.com' AND s.email = 'liam.garcia@houghton.edu') OR
    (p.email = 'carlos.rodriguez@email.com' AND s.email = 'isabella.rodriguez@calumet.edu') OR
    (p.email = 'amanda.taylor@email.com' AND s.email = 'lucas.taylor@calumet.edu')
)
JOIN students st ON st.user_id = s.id
WHERE p.role = 'parent' AND s.role = 'student'
ON CONFLICT (parent_id, student_id) DO UPDATE SET status = 'active';

-- Step 6: Create Associates (5 associates)
INSERT INTO users (email, password_hash, first_name, last_name, role) VALUES
('jennifer.parker@school.edu', '$2b$10$dummyhash', 'Jennifer', 'Parker', 'associate'),
('michael.johnson@school.edu', '$2b$10$dummyhash', 'Michael', 'Johnson', 'associate'),
('lisa.martinez@school.edu', '$2b$10$dummyhash', 'Lisa', 'Martinez', 'associate'),
('robert.smith@school.edu', '$2b$10$dummyhash', 'Robert', 'Smith', 'associate'),
('emily.davis@school.edu', '$2b$10$dummyhash', 'Emily', 'Davis', 'associate')
ON CONFLICT (email) DO UPDATE SET
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    role = EXCLUDED.role;

-- Create associate-school assignments
INSERT INTO associates_schools (associate_id, school_id)
SELECT u.id, s.id
FROM users u
CROSS JOIN schools s
WHERE u.role = 'associate' AND (
    (u.email = 'jennifer.parker@school.edu' AND s.id = 7) OR
    (u.email = 'michael.johnson@school.edu' AND s.id IN (7, 8)) OR
    (u.email = 'lisa.martinez@school.edu' AND s.id = 8) OR
    (u.email = 'robert.smith@school.edu' AND s.id = 9) OR
    (u.email = 'emily.davis@school.edu' AND s.id = 9)
)
ON CONFLICT (associate_id, school_id) DO NOTHING;

-- Step 7: Delete any users that are NOT in our sample data list
DELETE FROM students WHERE user_id NOT IN (
    SELECT id FROM users WHERE email IN (
        'emma.thompson@horizon.edu', 'james.martinez@horizon.edu', 'sophia.chen@horizon.edu',
        'michael.anderson@horizon.edu', 'olivia.williams@houghton.edu', 'noah.brown@houghton.edu',
        'ava.davis@houghton.edu', 'liam.garcia@houghton.edu', 'isabella.rodriguez@calumet.edu',
        'ethan.wilson@calumet.edu', 'mia.moore@calumet.edu', 'lucas.taylor@calumet.edu',
        'sarah.thompson@email.com', 'david.wilson@email.com', 'maria.martinez@email.com',
        'wei.chen@email.com', 'robert.anderson@email.com', 'jennifer.brown@email.com',
        'christopher.davis@email.com', 'patricia.garcia@email.com', 'carlos.rodriguez@email.com',
        'amanda.taylor@email.com', 'jennifer.parker@school.edu', 'michael.johnson@school.edu',
        'lisa.martinez@school.edu', 'robert.smith@school.edu', 'emily.davis@school.edu'
    )
);

DELETE FROM parent_child_links WHERE parent_id NOT IN (
    SELECT id FROM users WHERE email IN (
        'sarah.thompson@email.com', 'david.wilson@email.com', 'maria.martinez@email.com',
        'wei.chen@email.com', 'robert.anderson@email.com', 'jennifer.brown@email.com',
        'christopher.davis@email.com', 'patricia.garcia@email.com', 'carlos.rodriguez@email.com',
        'amanda.taylor@email.com'
    )
);

DELETE FROM associates_schools WHERE associate_id NOT IN (
    SELECT id FROM users WHERE email IN (
        'jennifer.parker@school.edu', 'michael.johnson@school.edu',
        'lisa.martinez@school.edu', 'robert.smith@school.edu', 'emily.davis@school.edu'
    )
);

DELETE FROM users WHERE email NOT IN (
    'emma.thompson@horizon.edu', 'james.martinez@horizon.edu', 'sophia.chen@horizon.edu',
    'michael.anderson@horizon.edu', 'olivia.williams@houghton.edu', 'noah.brown@houghton.edu',
    'ava.davis@houghton.edu', 'liam.garcia@houghton.edu', 'isabella.rodriguez@calumet.edu',
    'ethan.wilson@calumet.edu', 'mia.moore@calumet.edu', 'lucas.taylor@calumet.edu',
    'sarah.thompson@email.com', 'david.wilson@email.com', 'maria.martinez@email.com',
    'wei.chen@email.com', 'robert.anderson@email.com', 'jennifer.brown@email.com',
    'christopher.davis@email.com', 'patricia.garcia@email.com', 'carlos.rodriguez@email.com',
    'amanda.taylor@email.com', 'jennifer.parker@school.edu', 'michael.johnson@school.edu',
    'lisa.martinez@school.edu', 'robert.smith@school.edu', 'emily.davis@school.edu'
);

COMMIT;

-- Verify the data
SELECT 'Schools:' as type, COUNT(*)::text as count FROM schools
UNION ALL
SELECT 'Students:', COUNT(*)::text FROM students
UNION ALL
SELECT 'Parents:', COUNT(*)::text FROM users WHERE role = 'parent'
UNION ALL
SELECT 'Associates:', COUNT(*)::text FROM users WHERE role = 'associate';
