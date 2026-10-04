package com.example.cv.importing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CvDocumentImportRepository extends JpaRepository<CvDocumentImport, UUID> {
	Optional<CvDocumentImport> findByIdAndOwnerId(UUID id, UUID ownerId);

	boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}