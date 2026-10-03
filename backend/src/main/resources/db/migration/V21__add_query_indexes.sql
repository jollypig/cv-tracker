DROP INDEX IF EXISTS idx_person_owner_id;
CREATE INDEX idx_person_owner_name
    ON person (owner_id, last_name, first_name, id);

DROP INDEX IF EXISTS idx_cv_person_id;
CREATE INDEX idx_cv_person_updated_at
    ON cv (person_id, updated_at DESC);