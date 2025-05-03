-- Convert all timestamp columns to timestamptz in availabilities table
ALTER TABLE availabilities ALTER COLUMN start_time TYPE TIMESTAMPTZ;
ALTER TABLE availabilities ALTER COLUMN end_time TYPE TIMESTAMPTZ;
ALTER TABLE availabilities ALTER COLUMN created_at TYPE TIMESTAMPTZ;
ALTER TABLE availabilities ALTER COLUMN updated_at TYPE TIMESTAMPTZ;

-- Convert all timestamp columns to timestamptz in schedules table
ALTER TABLE schedules ALTER COLUMN week_start TYPE TIMESTAMPTZ;
ALTER TABLE schedules ALTER COLUMN created_at TYPE TIMESTAMPTZ;
ALTER TABLE schedules ALTER COLUMN updated_at TYPE TIMESTAMPTZ;

-- Convert all timestamp columns to timestamptz in shifts table
ALTER TABLE shifts ALTER COLUMN start_time TYPE TIMESTAMPTZ;
ALTER TABLE shifts ALTER COLUMN end_time TYPE TIMESTAMPTZ;
ALTER TABLE shifts ALTER COLUMN created_at TYPE TIMESTAMPTZ;
ALTER TABLE shifts ALTER COLUMN updated_at TYPE TIMESTAMPTZ; 