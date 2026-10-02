package com.example.cv.cv;

import java.time.Instant;
import java.util.UUID;

public record CvVersionResponse(
        UUID id,
        UUID cvId,
        int versionNumber,
        String description,
        Instant createdAt,
        UUID parentVersionId,
        UUID parentCvId,
        Integer parentVersionNumber) {

    public CvVersionResponse(UUID id, UUID cvId, int versionNumber, String description, Instant createdAt) {
        this(id, cvId, versionNumber, description, createdAt, null, null, null);
    }
}