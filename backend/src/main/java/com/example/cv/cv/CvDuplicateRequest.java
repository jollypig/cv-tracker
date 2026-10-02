package com.example.cv.cv;

import jakarta.validation.constraints.Size;

public record CvDuplicateRequest(@Size(max = 255) String name) {
}