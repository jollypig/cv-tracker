package com.example.cv.importing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

public record ParsedProject(
    @Valid ExtractedValue<String> company,
    @NotNull List<@NotNull @Valid ExtractedValue<String>> industries,
    @Valid ExtractedValue<String> projectName,
    @Valid ExtractedValue<String> projectDescription,
    @Valid ExtractedValue<String> startDate,
    @Valid ExtractedValue<String> endDate,
    @Valid ExtractedValue<String> position,
    @NotNull List<@NotNull @Valid ExtractedValue<String>> responsibilities,
    @NotNull List<@NotNull @Valid ExtractedValue<String>> technologiesAndTools
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