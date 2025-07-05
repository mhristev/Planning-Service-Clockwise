-- Add confirmation fields to work_sessions table
ALTER TABLE work_sessions ADD COLUMN IF NOT EXISTS confirmed BOOLEAN DEFAULT FALSE;
ALTER TABLE work_sessions ADD COLUMN IF NOT EXISTS confirmed_by VARCHAR(36);
ALTER TABLE work_sessions ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE work_sessions ADD COLUMN IF NOT EXISTS modified_by VARCHAR(36);
ALTER TABLE work_sessions ADD COLUMN IF NOT EXISTS original_clock_in_time TIMESTAMP WITH TIME ZONE;
ALTER TABLE work_sessions ADD COLUMN IF NOT EXISTS original_clock_out_time TIMESTAMP WITH TIME ZONE;

-- Create index for confirmation queries
CREATE INDEX IF NOT EXISTS idx_work_sessions_confirmed ON work_sessions(confirmed);
CREATE INDEX IF NOT EXISTS idx_work_sessions_confirmed_by ON work_sessions(confirmed_by); 