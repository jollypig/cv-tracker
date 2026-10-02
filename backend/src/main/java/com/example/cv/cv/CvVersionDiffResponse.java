package com.example.cv.cv;

import java.util.List;

public record CvVersionDiffResponse(
        CvVersionResponse fromVersion,
        CvVersionResponse toVersion,
        List<CvVersionChange> changes) {
}