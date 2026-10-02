CREATE TABLE cv_template (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    template_key VARCHAR(100) NOT NULL UNIQUE,
    version INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_cv_template_version_positive CHECK (version > 0)
);

INSERT INTO cv_template (id, name, description, template_key, version, active, created_at) VALUES
    ('00000000-0000-4000-8000-000000000001', 'Modern', 'A clear, contemporary layout with a strong visual hierarchy.', 'modern', 1, TRUE, CURRENT_TIMESTAMP),
    ('00000000-0000-4000-8000-000000000002', 'Classic', 'A traditional single-column layout for a formal presentation.', 'classic', 1, TRUE, CURRENT_TIMESTAMP),
    ('00000000-0000-4000-8000-000000000003', 'Minimal', 'A compact, typography-led layout with restrained decoration.', 'minimal', 1, TRUE, CURRENT_TIMESTAMP);