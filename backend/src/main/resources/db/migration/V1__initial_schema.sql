CREATE TABLE person (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    headline VARCHAR(255),
    photo_storage_key VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE person_contact (
    id UUID PRIMARY KEY,
    person_id UUID NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    value VARCHAR(500) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_person_contact_person_id ON person_contact(person_id);

CREATE TABLE cv (
    id UUID PRIMARY KEY,
    person_id UUID NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    language VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL,
    template_id UUID,
    current_version_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cv_person_id ON cv(person_id);