package com.example.cv.importing;

import java.util.List;
import java.util.Objects;

public record ParsedProject(
        ExtractedValue<String> company,
        List<ExtractedValue<String>> industries,
        ExtractedValue<String> projectName,
        ExtractedValue<String> projectDescription,
        ExtractedValue<String> startDate,
        ExtractedValue<String> endDate,
        ExtractedValue<String> position,
        List<ExtractedValue<String>> responsibilities,
        List<ExtractedValue<String>> technologiesAndTools
) {

    public ParsedProject {
        Objects.requireNonNull(industries, "industries");
        Objects.requireNonNull(responsibilities, "responsibilities");
        Objects.requireNonNull(technologiesAndTools, "technologiesAndTools");
        industries = List.copyOf(industries);
        responsibilities = List.copyOf(responsibilities);
        technologiesAndTools = List.copyOf(technologiesAndTools);
    }
}