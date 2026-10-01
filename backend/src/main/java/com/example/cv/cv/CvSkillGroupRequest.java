package com.example.cv.cv;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CvSkillGroupRequest(
        @NotBlank @Size(max = 255) String name,
        @PositiveOrZero int sortOrder) {
}