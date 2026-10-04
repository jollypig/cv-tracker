package com.example.cv.importing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ParsedSkill(
        ExtractedValue<String> name,
        ExtractedValue<String> group,
        List<ExtractedValue<String>> evidence,
        UUID canonicalSkillId,
        String canonicalName,
        BigDecimal yearsOfExperience,
        LocalDate lastUsedDate,
        boolean requiresReview
) {

    public ParsedSkill(
            ExtractedValue<String> name,
            ExtractedValue<String> group,
            List<ExtractedValue<String>> evidence
    ) {
        this(name, group, evidence, null, null, null, null, false);
    }

    public ParsedSkill {
        Objects.requireNonNull(evidence, "evidence");
        evidence = List.copyOf(evidence);
    }
}