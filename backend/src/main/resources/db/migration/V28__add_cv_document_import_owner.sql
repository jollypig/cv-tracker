ALTER TABLE cv_document_import
    ADD COLUMN owner_id UUID REFERENCES app_user(id) ON DELETE CASCADE;

CREATE INDEX idx_cv_document_import_owner_id
    ON cv_document_import (owner_id);