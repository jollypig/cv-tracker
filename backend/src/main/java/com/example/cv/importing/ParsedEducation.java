package com.example.cv.importing;

public record ParsedEducation(
        ExtractedValue<String> institution,
        ExtractedValue<String> degree,
        ExtractedValue<String> fieldOfStudy,
        ExtractedValue<String> startDate,
        ExtractedValue<String> endDate,
        ExtractedValue<String> description
) {
}