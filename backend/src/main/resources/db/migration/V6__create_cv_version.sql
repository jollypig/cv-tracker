CREATE TABLE cv_version (
    id UUID PRIMARY KEY,
    cv_id UUID NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    description VARCHAR(500),
    snapshot JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_cv_version_number UNIQUE (cv_id, version_number),
    CONSTRAINT ck_cv_version_number_positive CHECK (version_number > 0)
);

CREATE INDEX idx_cv_version_cv_id_version_number
    ON cv_version (cv_id, version_number DESC);