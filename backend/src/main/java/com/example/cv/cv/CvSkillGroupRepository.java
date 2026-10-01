package com.example.cv.cv;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CvSkillGroupRepository extends JpaRepository<CvSkillGroup, UUID> {
    List<CvSkillGroup> findByCvIdOrderBySortOrderAsc(UUID cvId);
    Optional<CvSkillGroup> findByIdAndCvId(UUID id, UUID cvId);
}