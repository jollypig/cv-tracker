package com.example.cv.importing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CvDocumentImportRepository extends JpaRepository<CvDocumentImport, UUID> {
}