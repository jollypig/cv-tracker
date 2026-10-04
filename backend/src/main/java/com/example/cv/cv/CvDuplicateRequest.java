package com.example.cv.cv;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CvDuplicateRequest(@Size(max = 255) String name, UUID personId) {
}