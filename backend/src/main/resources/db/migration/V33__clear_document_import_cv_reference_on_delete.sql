ALTER TABLE cv_document_import
    DROP CONSTRAINT cv_document_import_cv_id_fkey,
    ADD CONSTRAINT cv_document_import_cv_id_fkey
        FOREIGN KEY (cv_id) REFERENCES cv(id) ON DELETE SET NULL;