-- SprintPilot Database Schema
-- PostgreSQL compatible schema

-- Sprint table
CREATE TABLE IF NOT EXISTS sprint (
    id VARCHAR(255) PRIMARY KEY,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    duration INTEGER NOT NULL,
    freeze_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Sprint Events (Deployments, Meetings, etc.)
CREATE TABLE IF NOT EXISTS sprint_event (
    id VARCHAR(255) PRIMARY KEY,
    sprint_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- DEPLOYMENT, MEETING, HOLIDAY
    event_subtype VARCHAR(50), -- For meetings: PLANNING, GROOMING, RETROSPECTIVE
    name VARCHAR(255) NOT NULL,
    event_date DATE NOT NULL,
    event_time TIME,
    duration_minutes INTEGER,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sprint_id) REFERENCES sprint(id) ON DELETE CASCADE
);

-- Team Member table
CREATE TABLE IF NOT EXISTS team_member (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    daily_capacity DECIMAL(5,2) NOT NULL DEFAULT 6.0,
    email VARCHAR(255),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Sprint Team Association (Many-to-Many)
CREATE TABLE IF NOT EXISTS sprint_team (
    sprint_id VARCHAR(255) NOT NULL,
    member_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (sprint_id, member_id),
    FOREIGN KEY (sprint_id) REFERENCES sprint(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES team_member(id) ON DELETE CASCADE
);

-- Leave Days
CREATE TABLE IF NOT EXISTS leave_day (
    id VARCHAR(255) PRIMARY KEY,
    member_id VARCHAR(255) NOT NULL,
    sprint_id VARCHAR(255),
    leave_date DATE NOT NULL,
    leave_type VARCHAR(50) DEFAULT 'PERSONAL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES team_member(id) ON DELETE CASCADE,
    FOREIGN KEY (sprint_id) REFERENCES sprint(id) ON DELETE SET NULL
);

-- Task table
CREATE TABLE IF NOT EXISTS task (
    id VARCHAR(255) PRIMARY KEY,
    sprint_id VARCHAR(255) NOT NULL,
    task_key VARCHAR(50) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    description TEXT,
    story_points DECIMAL(5,2) NOT NULL DEFAULT 0,
    category VARCHAR(50) NOT NULL, -- FEATURE, TECH_DEBT, PROD_ISSUE
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    status VARCHAR(50) DEFAULT 'TODO',
    start_date DATE,
    due_date DATE,
    time_spent DECIMAL(5,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sprint_id) REFERENCES sprint(id) ON DELETE CASCADE
);

-- Task Assignment
CREATE TABLE IF NOT EXISTS task_assignment (
    task_id VARCHAR(255) NOT NULL,
    member_id VARCHAR(255) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id, member_id),
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES team_member(id) ON DELETE CASCADE
);

-- Holiday Master table
CREATE TABLE IF NOT EXISTS holiday (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    holiday_date DATE NOT NULL,
    holiday_type VARCHAR(50) DEFAULT 'PUBLIC', -- PUBLIC, COMPANY
    recurring BOOLEAN DEFAULT FALSE,
    location JSONB, -- JSON array of locations: ["BANGALORE", "COIMBATORE"] or NULL for all locations
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Sprint History (for completed sprints)
CREATE TABLE IF NOT EXISTS sprint_history (
    id VARCHAR(255) PRIMARY KEY,
    sprint_id VARCHAR(255) NOT NULL,
    completed_date TIMESTAMP NOT NULL,
    total_story_points DECIMAL(10,2),
    completed_story_points DECIMAL(10,2),
    team_size INTEGER,
    velocity DECIMAL(10,2),
    feature_percentage DECIMAL(5,2),
    tech_debt_percentage DECIMAL(5,2),
    prod_issue_percentage DECIMAL(5,2),
    summary_json TEXT, -- Store complete sprint data as JSON
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sprint_id) REFERENCES sprint(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_sprint_dates ON sprint(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_sprint_event_date ON sprint_event(event_date);
CREATE INDEX IF NOT EXISTS idx_task_sprint ON task(sprint_id);
CREATE INDEX IF NOT EXISTS idx_task_category ON task(category);
CREATE INDEX IF NOT EXISTS idx_leave_day_member ON leave_day(member_id);
CREATE INDEX IF NOT EXISTS idx_holiday_date ON holiday(holiday_date);
CREATE INDEX IF NOT EXISTS idx_holiday_location_gin ON holiday USING GIN (location);
