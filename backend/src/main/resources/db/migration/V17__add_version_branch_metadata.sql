ALTER TABLE cv_version
    ADD COLUMN parent_version_id UUID REFERENCES cv_version(id) ON DELETE SET NULL;

CREATE INDEX idx_cv_version_parent_version_id
    ON cv_version (parent_version_id);