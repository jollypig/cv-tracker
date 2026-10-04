package com.example.cv.importing;

import java.time.Instant;
import java.util.UUID;

public record CvDocumentImportResponse(
        UUID importId,
        String fileName,
        String mediaType,
        long fileSize,
        CvImportStatus status,
        String errorMessage,
        ParsedCv result,
        UUID cvId,
        Instant createdAt,
        Instant updatedAt
) {
}