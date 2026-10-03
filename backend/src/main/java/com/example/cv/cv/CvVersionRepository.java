package com.example.cv.cv;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CvVersionRepository extends JpaRepository<CvVersion, UUID> {
    List<CvVersion> findAllByCv_IdOrderByVersionNumberDesc(UUID cvId);
    Optional<CvVersion> findByCv_IdAndVersionNumber(UUID cvId, int versionNumber);
    Optional<CvVersion> findTopByCv_IdOrderByVersionNumberDesc(UUID cvId);
    boolean existsByIdAndCv_Person_Owner_Id(UUID id, UUID ownerId);
}