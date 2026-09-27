CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    system_role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_users_email ON users(email);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_refresh_token ON refresh_tokens(token);

CREATE TABLE projects (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE project_members (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_in_project VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP NOT NULL,
    UNIQUE(project_id, user_id)
);
CREATE INDEX idx_project_members_project ON project_members(project_id);
CREATE INDEX idx_project_members_user ON project_members(user_id);

CREATE TABLE sprints (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    sprint_number INT,
    planned_task_count INT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) NOT NULL
);
CREATE INDEX idx_sprints_project ON sprints(project_id);

CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    sprint_id UUID NOT NULL REFERENCES sprints(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    deadline TIMESTAMP,
    extended_deadline TIMESTAMP,
    assigned_to UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL
);
CREATE INDEX idx_tasks_sprint ON tasks(sprint_id);
CREATE INDEX idx_tasks_assigned_to ON tasks(assigned_to);

CREATE TABLE submissions (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    submitted_by UUID NOT NULL REFERENCES users(id),
    file_path VARCHAR(500),
    submitted_at TIMESTAMP NOT NULL,
    timing VARCHAR(10),
    feedback TEXT
);
CREATE INDEX idx_submissions_task ON submissions(task_id);
CREATE INDEX idx_submissions_user ON submissions(submitted_by);
