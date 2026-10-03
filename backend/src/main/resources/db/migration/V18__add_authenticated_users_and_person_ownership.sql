CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    issuer VARCHAR(500) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    email VARCHAR(320),
    display_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_user_issuer_subject UNIQUE (issuer, subject)
);

ALTER TABLE person
    ADD COLUMN owner_id UUID REFERENCES app_user(id) ON DELETE CASCADE;

CREATE INDEX idx_person_owner_id ON person(owner_id);