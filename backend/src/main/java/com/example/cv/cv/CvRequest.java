package com.example.cv.cv;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CvRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotBlank @Size(max = 10) String language,
        @NotNull CvStatus status) {
}