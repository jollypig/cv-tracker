package com.example.cv.cv;

import java.util.UUID;

public record CvVersionSnapshot(
        UUID templateId,
        String name,
        String description,
        String language,
        CvStatus status,
        CvContent content) {
}