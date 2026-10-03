package com.example.cv.cv;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CvExportRepository extends JpaRepository<CvExport, UUID> {
    List<CvExport> findAllByVersion_IdOrderByCreatedAtDesc(UUID versionId);
    boolean existsByIdAndVersion_Cv_Person_Owner_Id(UUID id, UUID ownerId);
}