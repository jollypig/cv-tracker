package com.example.cv.importing;

public record DetectedCvSection(String type, int start, int end) {

    public CvSectionType sectionType() {
        return CvSectionType.fromName(type);
    }
}