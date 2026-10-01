CREATE TABLE cv_education (
    id UUID PRIMARY KEY,
    cv_id UUID NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    institution VARCHAR(255) NOT NULL,
    degree VARCHAR(255),
    field_of_study VARCHAR(255),
    start_date DATE,
    end_date DATE,
    description TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_cv_education_cv_id ON cv_education(cv_id);