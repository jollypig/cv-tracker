ALTER TABLE cv ADD COLUMN include_skill_details_in_output boolean NOT NULL DEFAULT false;

UPDATE cv
SET include_skill_details_in_output = true
WHERE EXISTS (
    SELECT 1
    FROM cv_skill_group skill_group
    JOIN cv_skill skill ON skill.skill_group_id = skill_group.id
    WHERE skill_group.cv_id = cv.id
      AND skill.details ->> 'includeInOutput' = 'true'
);

UPDATE cv_version
SET snapshot = jsonb_set(snapshot, '{content,includeSkillDetailsInOutput}', 'true'::jsonb, true)
WHERE snapshot #> '{content,includeSkillDetailsInOutput}' IS NULL
  AND jsonb_path_exists(snapshot,
      '$.content.skillGroups[*].skills[*].details.includeInOutput ? (@ == true)');