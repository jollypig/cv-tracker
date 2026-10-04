package com.example.cv.cv;

import java.time.Instant;

public record CvShareCreated(String token, Instant createdAt) {
}