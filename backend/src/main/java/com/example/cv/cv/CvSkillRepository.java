package com.example.cv.cv;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CvSkillRepository extends JpaRepository<CvSkill, UUID> {
    List<CvSkill> findBySkillGroupIdOrderBySortOrderAsc(UUID skillGroupId);
    Optional<CvSkill> findByIdAndSkillGroupId(UUID id, UUID skillGroupId);
}