package com.example.cv.cv;

import jakarta.validation.constraints.Size;

public record CvVersionRequest(@Size(max = 500) String description) {
}