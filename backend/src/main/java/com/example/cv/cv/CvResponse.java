package com.example.cv.cv;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CvResponse(
        UUID id,
        UUID personId,
        String personName,
        String name,
        String description,
        String language,
        CvStatus status,
        UUID templateId,
        UUID currentVersionId,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt) {
}