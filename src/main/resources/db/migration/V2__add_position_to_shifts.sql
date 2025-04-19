-- Add position column to shifts table
ALTER TABLE shifts ADD COLUMN IF NOT EXISTS position VARCHAR(50); 