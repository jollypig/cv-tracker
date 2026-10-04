CREATE TABLE cv_document_import (
    id UUID PRIMARY KEY,
    file_name VARCHAR(500) NOT NULL,
    media_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    result_json TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_cv_document_import_file_size CHECK (file_size > 0),
    CONSTRAINT ck_cv_document_import_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'NEEDS_REVIEW', 'FAILED'))
);

CREATE INDEX idx_cv_document_import_created_at
    ON cv_document_import (created_at DESC);