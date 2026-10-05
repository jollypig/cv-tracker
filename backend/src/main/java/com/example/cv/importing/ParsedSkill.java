package com.example.cv.importing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ParsedSkill(
    @Valid ExtractedValue<String> name,
    @Valid ExtractedValue<String> group,
    @NotNull List<@NotNull @Valid ExtractedValue<String>> evidence,
        UUID canonicalSkillId,
        String canonicalName,
        BigDecimal yearsOfExperience,
        LocalDate lastUsedDate,
        boolean requiresReview,
        @Valid ExtractedValue<String> level
) {

        public ParsedSkill(
            ExtractedValue<String> name,
            ExtractedValue<String> group,
            List<ExtractedValue<String>> evidence,
            UUID canonicalSkillId,
            String canonicalName,
            BigDecimal yearsOfExperience,
            LocalDate lastUsedDate,
            boolean requiresReview
        ) {
        this(name, group, evidence, canonicalSkillId, canonicalName, yearsOfExperience,
            lastUsedDate, requiresReview, null);
        }

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