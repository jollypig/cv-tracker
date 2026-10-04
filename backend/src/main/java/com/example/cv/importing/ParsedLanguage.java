package com.example.cv.importing;

import jakarta.validation.Valid;

public record ParsedLanguage(
        @Valid ExtractedValue<String> name,
        @Valid ExtractedValue<String> proficiency
) {
}