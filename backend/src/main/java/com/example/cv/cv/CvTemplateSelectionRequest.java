package com.example.cv.cv;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CvTemplateSelectionRequest(@NotNull UUID templateId) {
}