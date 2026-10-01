CREATE TABLE cv_experience (
    id UUID PRIMARY KEY,
    cv_id UUID NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    company VARCHAR(255) NOT NULL,
    position VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    start_date DATE,
    end_date DATE,
    current BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_cv_experience_cv_id ON cv_experience(cv_id);