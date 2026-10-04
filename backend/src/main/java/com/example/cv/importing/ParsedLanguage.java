package com.example.cv.importing;

public record ParsedLanguage(
        ExtractedValue<String> name,
        ExtractedValue<String> proficiency
) {
}