ALTER TABLE cv_skill ADD COLUMN details jsonb;
ALTER TABLE cv_experience_project ADD COLUMN project_key varchar(100);
ALTER TABLE cv_project ADD COLUMN project_key varchar(100);
ALTER TABLE cv_project ADD COLUMN period_from date;
ALTER TABLE cv_project ADD COLUMN period_to date;
ALTER TABLE cv_project ADD COLUMN current boolean NOT NULL DEFAULT false;
UPDATE cv_experience_project SET project_key = id::text;
UPDATE cv_project SET project_key = id::text;