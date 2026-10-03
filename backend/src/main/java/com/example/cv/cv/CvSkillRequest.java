package com.example.cv.cv;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CvSkillRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 50) String level,
        @PositiveOrZero int sortOrder,
                Boolean visible,
                @jakarta.validation.Valid CvSkillDetails details) {
        public CvSkillRequest(String name, String level, int sortOrder, Boolean visible) {
                this(name, level, sortOrder, visible, null);
        }
}