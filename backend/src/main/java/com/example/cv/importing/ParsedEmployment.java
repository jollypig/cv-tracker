package com.example.cv.importing;

import java.util.List;
import java.util.Objects;

public record ParsedEmployment(
        ExtractedValue<String> company,
        ExtractedValue<String> position,
        ExtractedValue<String> startDate,
        ExtractedValue<String> endDate,
        ExtractedValue<String> location,
        ExtractedValue<String> employmentType,
        ExtractedValue<String> industry,
        List<ParsedProject> projects
) {

    public ParsedEmployment {
        Objects.requireNonNull(projects, "projects");
        projects = List.copyOf(projects);
    }
}