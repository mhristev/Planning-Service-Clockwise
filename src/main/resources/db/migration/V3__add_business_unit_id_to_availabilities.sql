-- Add business_unit_id column to availabilities table
ALTER TABLE availabilities ADD COLUMN IF NOT EXISTS business_unit_id VARCHAR(50); 