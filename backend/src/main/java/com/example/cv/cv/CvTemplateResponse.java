package com.example.cv.cv;

import java.time.Instant;
import java.util.UUID;

public record CvTemplateResponse(
        UUID id,
        String name,
        String description,
        String templateKey,
        int version,
        boolean active,
        Instant createdAt) {

    static CvTemplateResponse from(CvTemplate template) {
        return new CvTemplateResponse(template.getId(), template.getName(), template.getDescription(),
                template.getTemplateKey(), template.getVersion(), template.isActive(), template.getCreatedAt());
    }
}