ALTER TABLE cv ADD COLUMN include_skill_levels_in_output boolean NOT NULL DEFAULT true;

UPDATE cv_version
SET snapshot = jsonb_set(snapshot, '{content,includeSkillLevelsInOutput}', 'true'::jsonb, true)
WHERE snapshot #> '{content,includeSkillLevelsInOutput}' IS NULL;