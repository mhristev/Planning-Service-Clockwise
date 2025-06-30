-- Planning Service Database Schema
-- Consolidated migration including all tables, columns, and constraints

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Availabilities table with business_unit_id and timezone support
CREATE TABLE IF NOT EXISTS availabilities (
    id VARCHAR(50) DEFAULT uuid_generate_v4()::text PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    business_unit_id VARCHAR(50),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Schedules table with business_unit_id (renamed from restaurant_id) and timezone support
CREATE TABLE IF NOT EXISTS schedules (
    id VARCHAR(50) DEFAULT uuid_generate_v4()::text PRIMARY KEY,
    business_unit_id VARCHAR(50) NOT NULL,
    week_start TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Shifts table with position column and timezone support
CREATE TABLE IF NOT EXISTS shifts (
    id VARCHAR(50) DEFAULT uuid_generate_v4()::text PRIMARY KEY,
    schedule_id VARCHAR(50) NOT NULL,
    employee_id VARCHAR(50) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    position VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_schedule FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE CASCADE
);

-- Work sessions table for workload functionality
CREATE TABLE IF NOT EXISTS work_sessions (
    id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    user_id VARCHAR(36) NOT NULL,
    shift_id VARCHAR(36) NOT NULL,
    clock_in_time TIMESTAMP WITH TIME ZONE NOT NULL,
    clock_out_time TIMESTAMP WITH TIME ZONE,
    total_minutes INT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_shift_id UNIQUE (shift_id),
    CONSTRAINT fk_work_sessions_shift FOREIGN KEY (shift_id) REFERENCES shifts(id) ON DELETE CASCADE
);

-- Session notes table with unique constraint for one-to-one relationship
CREATE TABLE IF NOT EXISTS session_notes (
    id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    work_session_id VARCHAR(36) NOT NULL REFERENCES work_sessions(id),
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_work_session_id UNIQUE (work_session_id)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_work_sessions_user_id ON work_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_work_sessions_shift_id ON work_sessions(shift_id);
CREATE INDEX IF NOT EXISTS idx_work_sessions_status ON work_sessions(status);
CREATE INDEX IF NOT EXISTS idx_session_notes_work_session_id ON session_notes(work_session_id);