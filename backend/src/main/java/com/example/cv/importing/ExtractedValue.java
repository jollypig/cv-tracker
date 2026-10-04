package com.example.cv.importing;

import java.util.Objects;

public record ExtractedValue<T>(T value, Double confidence, String sourceText) {

    public ExtractedValue {
        Objects.requireNonNull(value, "value");
        if (confidence != null && (!Double.isFinite(confidence) || confidence < 0 || confidence > 1)) {
            throw new IllegalArgumentException("Confidence must be between 0 and 1");
        }
    }
}