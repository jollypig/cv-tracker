package com.example.cv.importing;

import java.util.List;
import java.util.Objects;

public record DetectedCvSections(List<DetectedCvSection> sections) {

    public DetectedCvSections {
        Objects.requireNonNull(sections, "sections");
        sections = List.copyOf(sections);
    }
}