package com.example.cv.cv;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public record CvSkillDetails(
        @DecimalMin("0") BigDecimal yearsOfExperience,
        @DecimalMin("0") BigDecimal yearsActivelyUsed,
        @Size(max = 10) String lastUsed,
        @Size(max = 10) String startedFrom,
        @Pattern(regexp = "daily|occasionally|rarely") String frequency,
        @Pattern(regexp = "active|learning|maintaining|deprecated") String status,
        @Size(max = 100) List<@jakarta.validation.constraints.NotNull @Valid ProjectLink> linkedProjects,
        boolean includeInOutput) {

    public record ProjectLink(@jakarta.validation.constraints.NotBlank @Size(max = 100) String projectKey,
                              @Size(max = 1000) String outcome) {
    }

    public record ProjectPeriod(String projectKey, LocalDate start, LocalDate end, boolean current) {
    }

    public record Calculated(BigDecimal yearsOfExperience, BigDecimal totalExperience,
                             LocalDate lastUsed, boolean stale) {
    }

    @AssertTrue(message = "Skill dates must be a year or ISO date, with started from not after last used")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isDatesValid() {
        try {
            LocalDate start = date(startedFrom);
            LocalDate end = date(lastUsed);
            return start == null || end == null || !start.isAfter(end);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public Calculated calculate(List<ProjectPeriod> projects, LocalDate today) {
        LocalDate latest = date(lastUsed);
        long days = 0;
        List<String> keys = linkedProjects == null ? List.of() : linkedProjects.stream()
                .map(ProjectLink::projectKey).distinct().toList();
        for (ProjectPeriod project : projects) {
            if (!keys.contains(project.projectKey())) {
                continue;
            }
            LocalDate end = project.current() ? today : project.end();
            if (end != null && (latest == null || end.isAfter(latest))) {
                latest = end;
            }
            if (project.start() != null && end != null && !end.isBefore(project.start())) {
                days += ChronoUnit.DAYS.between(project.start(), end);
            }
        }
        LocalDate start = date(startedFrom);
        BigDecimal years = yearsOfExperience;
        if (years == null && start != null && latest != null && !latest.isBefore(start)) {
            years = years(ChronoUnit.DAYS.between(start, latest));
        }
        BigDecimal projectYears = years(days);
        BigDecimal total = days == 0 ? years : years == null ? projectYears : years.max(projectYears);
        return new Calculated(years == null ? null : years.setScale(0, RoundingMode.CEILING),
            total == null ? null : total.setScale(0, RoundingMode.CEILING),
            latest, latest != null && latest.isBefore(today.minusYears(5)));
    }

    private static BigDecimal years(long days) {
        return BigDecimal.valueOf(days).divide(new BigDecimal("365.2425"), 2, RoundingMode.HALF_UP);
    }

    private static LocalDate date(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!value.matches("\\d{4}(-\\d{2}-\\d{2})?")) {
            throw new IllegalArgumentException("Expected a year or ISO date");
        }
        return LocalDate.parse(value.length() == 4 ? value + "-01-01" : value);
    }
}