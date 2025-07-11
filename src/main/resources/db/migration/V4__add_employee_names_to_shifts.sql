-- Add employee name fields to shifts table for inter-service communication
-- These fields will be populated asynchronously from User Service via Kafka

ALTER TABLE shifts 
ADD COLUMN IF NOT EXISTS employee_first_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS employee_last_name VARCHAR(255);

-- Add comment for documentation
COMMENT ON COLUMN shifts.employee_first_name IS 'First name of employee, populated asynchronously from User Service';
COMMENT ON COLUMN shifts.employee_last_name IS 'Last name of employee, populated asynchronously from User Service';
