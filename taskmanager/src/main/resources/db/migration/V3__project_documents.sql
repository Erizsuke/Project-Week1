CREATE TABLE project_documents (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    uploaded_by UUID NOT NULL REFERENCES users(id),
    original_filename VARCHAR(500) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL UNIQUE,
    media_type VARCHAR(255),
    file_size BIGINT NOT NULL,
    extracted_text TEXT NOT NULL DEFAULT '',
    uploaded_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_project_documents_project ON project_documents(project_id);
CREATE INDEX idx_project_documents_uploaded_at ON project_documents(uploaded_at DESC);