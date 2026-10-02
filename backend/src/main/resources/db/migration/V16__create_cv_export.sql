CREATE TABLE cv_export (
    id UUID PRIMARY KEY,
    cv_version_id UUID NOT NULL REFERENCES cv_version(id) ON DELETE CASCADE,
    file_name VARCHAR(500) NOT NULL,
    storage_key VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_cv_export_file_size_nonnegative CHECK (file_size >= 0)
);

CREATE INDEX idx_cv_export_version_created_at
    ON cv_export (cv_version_id, created_at DESC);