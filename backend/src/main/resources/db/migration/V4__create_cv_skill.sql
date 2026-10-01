CREATE TABLE cv_skill_group (
    id UUID PRIMARY KEY,
    cv_id UUID NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_cv_skill_group_cv_id ON cv_skill_group(cv_id);

CREATE TABLE cv_skill (
    id UUID PRIMARY KEY,
    skill_group_id UUID NOT NULL REFERENCES cv_skill_group(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    level VARCHAR(50),
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_cv_skill_group_id ON cv_skill(skill_group_id);