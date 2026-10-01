ALTER TABLE cv ADD COLUMN summary TEXT;

CREATE TABLE cv_experience_project (
    id UUID PRIMARY KEY,
    experience_id UUID NOT NULL REFERENCES cv_experience(id) ON DELETE CASCADE,
    company VARCHAR(255),
    industries TEXT,
    project_name VARCHAR(255) NOT NULL,
    project_description TEXT,
    period_from DATE,
    period_to DATE,
    position VARCHAR(255),
    responsibilities TEXT,
    technologies TEXT,
    team_size INTEGER,
    external_link VARCHAR(1000),
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_cv_experience_project_experience_id ON cv_experience_project(experience_id);

CREATE TABLE cv_language (
    id UUID PRIMARY KEY,
    cv_id UUID NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    language VARCHAR(100) NOT NULL,
    level VARCHAR(50),
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE cv_project (
    id UUID PRIMARY KEY,
    cv_id UUID NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(255),
    description TEXT,
    technologies TEXT,
    url VARCHAR(500),
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE cv_certification (
    id UUID PRIMARY KEY,
    cv_id UUID NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    issuer VARCHAR(255),
    issue_date DATE,
    expiry_date DATE,
    credential_id VARCHAR(255),
    credential_url VARCHAR(500),
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE cv_custom_section (
    id UUID PRIMARY KEY,
    cv_id UUID NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE cv_section (
    id UUID PRIMARY KEY,
    cv_id UUID NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    section_type VARCHAR(50) NOT NULL,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL,
    CONSTRAINT uq_cv_section_type UNIQUE (cv_id, section_type)
);

CREATE INDEX idx_cv_language_cv_id ON cv_language(cv_id);
CREATE INDEX idx_cv_project_cv_id ON cv_project(cv_id);
CREATE INDEX idx_cv_certification_cv_id ON cv_certification(cv_id);
CREATE INDEX idx_cv_custom_section_cv_id ON cv_custom_section(cv_id);
CREATE INDEX idx_cv_section_cv_id ON cv_section(cv_id);