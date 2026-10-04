package com.example.cv.importing;

import com.example.cv.cv.CvStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CvDraftApprovalRequest(
        @NotNull UUID personId,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 10) String language,
        @NotNull CvStatus status,
        @Size(max = 20) List<@NotBlank @Size(max = 50) String> tags) {
}