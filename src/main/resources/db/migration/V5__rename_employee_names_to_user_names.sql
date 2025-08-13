-- Rename employee name columns to user name columns in shifts table
-- This aligns with the user-centric terminology across the system

ALTER TABLE shifts 
RENAME COLUMN employee_first_name TO user_first_name;

ALTER TABLE shifts 
RENAME COLUMN employee_last_name TO user_last_name;

-- Update column comments for documentation
COMMENT ON COLUMN shifts.user_first_name IS 'First name of user, populated asynchronously from User Service';
COMMENT ON COLUMN shifts.user_last_name IS 'Last name of user, populated asynchronously from User Service';