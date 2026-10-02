package com.example.cv.cv;

import java.time.Instant;
import java.util.UUID;

public record CvVersionResponse(
        UUID id,
        UUID cvId,
        int versionNumber,
        String description,
        Instant createdAt) {
}