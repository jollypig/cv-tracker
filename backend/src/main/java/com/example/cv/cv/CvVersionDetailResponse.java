package com.example.cv.cv;

import java.time.Instant;
import java.util.UUID;

public record CvVersionDetailResponse(
        UUID id,
        UUID cvId,
        int versionNumber,
        String description,
        Instant createdAt,
        CvVersionSnapshot snapshot) {
}