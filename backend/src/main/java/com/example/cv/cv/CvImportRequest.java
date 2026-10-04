package com.example.cv.cv;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CvImportRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotBlank @Size(max = 10) String language,
        @NotNull CvStatus status,
        @Size(max = 20) List<@NotBlank @Size(max = 50) String> tags,
        @NotNull @Valid CvContent content) {
}