package com.example.cv.importing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

public record ParsedEmployment(
    @Valid ExtractedValue<String> company,
    @Valid ExtractedValue<String> position,
    @Valid ExtractedValue<String> startDate,
    @Valid ExtractedValue<String> endDate,
    @Valid ExtractedValue<String> location,
    @Valid ExtractedValue<String> employmentType,
    @Valid ExtractedValue<String> industry,
    @NotNull List<@NotNull @Valid ParsedProject> projects
) {

    public ParsedEmployment {
        Objects.requireNonNull(projects, "projects");
        projects = List.copyOf(projects);
    }
}