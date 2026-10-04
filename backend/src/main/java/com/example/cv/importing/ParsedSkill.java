package com.example.cv.importing;

import java.util.List;
import java.util.Objects;

public record ParsedSkill(
        ExtractedValue<String> name,
        ExtractedValue<String> group,
        List<ExtractedValue<String>> evidence
) {

    public ParsedSkill {
        Objects.requireNonNull(evidence, "evidence");
        evidence = List.copyOf(evidence);
    }
}