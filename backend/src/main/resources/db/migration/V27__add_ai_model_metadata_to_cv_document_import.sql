ALTER TABLE cv_document_import
    ADD COLUMN ai_model_name VARCHAR(250),
    ADD COLUMN ai_model_version VARCHAR(250);