package com.example.cv.person;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PersonContactRequest(
        @NotNull ContactType type,
        @NotBlank @Size(max = 500) String value,
        boolean primary,
        @PositiveOrZero int sortOrder) {
}