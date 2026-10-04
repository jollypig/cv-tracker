ALTER TABLE cv_document_import
    ADD COLUMN cv_id UUID UNIQUE REFERENCES cv(id);

ALTER TABLE cv_document_import
    DROP CONSTRAINT ck_cv_document_import_status,
    ADD CONSTRAINT ck_cv_document_import_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'NEEDS_REVIEW', 'APPROVED', 'FAILED'));