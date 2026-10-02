package com.example.cv.cv;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CvTemplateRepository extends JpaRepository<CvTemplate, UUID> {
    List<CvTemplate> findAllByActiveTrueOrderByNameAsc();
    Optional<CvTemplate> findByIdAndActiveTrue(UUID id);
}