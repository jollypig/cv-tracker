package com.example.cv.importing;

import jakarta.validation.Valid;

public record ParsedEducation(
        @Valid ExtractedValue<String> institution,
        @Valid ExtractedValue<String> degree,
        @Valid ExtractedValue<String> fieldOfStudy,
        @Valid ExtractedValue<String> startDate,
        @Valid ExtractedValue<String> endDate,
        @Valid ExtractedValue<String> description
) {
}