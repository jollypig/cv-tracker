package com.example.cv.cv;

import java.time.Instant;

public record CvShareStatus(boolean enabled, Instant createdAt, long viewCount, Instant lastViewedAt) {
}