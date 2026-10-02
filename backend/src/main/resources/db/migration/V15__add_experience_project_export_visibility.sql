ALTER TABLE cv_experience_project
    ADD COLUMN show_project_name BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN show_customer_company BOOLEAN NOT NULL DEFAULT TRUE;