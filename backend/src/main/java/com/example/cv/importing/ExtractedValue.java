package com.example.cv.importing;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

public record ExtractedValue<T>(
    @NotNull T value,
    @DecimalMin("0.0") @DecimalMax("1.0") Double confidence,
    String sourceText
) {

    public ExtractedValue {
        Objects.requireNonNull(value, "value");
        if (confidence != null && !Double.isFinite(confidence)) {
            throw new IllegalArgumentException("Confidence must be finite");
        }
    }
}