package com.example.cv.importing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

final class ParsedCvNormalizer {

    private static final Pattern YEAR = Pattern.compile("\\d{4}");
    private static final Pattern YEAR_MONTH = Pattern.compile("\\d{4}-\\d{2}");
    private static final Pattern FULL_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern CEFR_LEVEL = Pattern.compile("(?i)(?<![A-Z0-9])(A1|A2|B1|B2|C1|C2)\\+?(?![A-Z0-9])");
    private static final Map<String, String> LANGUAGE_NAMES = Map.ofEntries(
            Map.entry("en", "English"), Map.entry("eng", "English"), Map.entry("english", "English"),
            Map.entry("fr", "French"), Map.entry("fra", "French"), Map.entry("french", "French"),
            Map.entry("français", "French"), Map.entry("de", "German"), Map.entry("deu", "German"),
            Map.entry("german", "German"), Map.entry("deutsch", "German"), Map.entry("es", "Spanish"),
            Map.entry("spa", "Spanish"), Map.entry("spanish", "Spanish"), Map.entry("español", "Spanish"),
            Map.entry("it", "Italian"), Map.entry("ita", "Italian"), Map.entry("italian", "Italian"),
            Map.entry("ru", "Russian"), Map.entry("rus", "Russian"), Map.entry("russian", "Russian")
    );

    private ParsedCvNormalizer() {
    }

    static ParsedCv normalize(ParsedCv parsedCv, LocalDate today) {
        return normalize(parsedCv, today, "");
    }

    static ParsedCv normalize(ParsedCv parsedCv, LocalDate today, String sourceText) {
        List<String> warnings = new ArrayList<>(parsedCv.warnings());
        List<ParsedProject> projects = parsedCv.projects();
        List<ParsedEmployment> employment = associateProjects(parsedCv.employment(), projects, warnings);
        validateDateRanges(employment, projects, parsedCv.education(), warnings);

        List<ParsedLanguage> languages = parsedCv.languages().stream()
            .map(language -> normalizeLanguage(language, sourceText, warnings))
                .toList();
        List<ParsedSkill> skills = mergeSkills(parsedCv.skills(), employment, projects);
        skills = skills.stream().map(skill -> calculateExperience(skill, employment, projects, today)).toList();

        return new ParsedCv(parsedCv.personalData(), parsedCv.professionalSummary(), employment, projects,
                parsedCv.education(), languages, skills, warnings.stream().distinct().toList());
    }

    private static List<ParsedEmployment> associateProjects(
            List<ParsedEmployment> employment,
            List<ParsedProject> projects,
            List<String> warnings
    ) {
        List<List<ParsedProject>> associated = new ArrayList<>();
        employment.forEach(record -> associated.add(new ArrayList<>(record.projects())));
        for (ParsedProject project : projects) {
            String projectCompany = value(project.company());
            if (projectCompany == null) {
                if (employment.size() > 1) {
                    warnings.add("Project employment association is ambiguous for " + value(project.projectName()));
                }
                continue;
            }
            List<Integer> matches = new ArrayList<>();
            for (int index = 0; index < employment.size(); index++) {
                if (sameText(projectCompany, value(employment.get(index).company()))) {
                    matches.add(index);
                }
            }
            if (matches.size() == 1) {
                List<ParsedProject> recordProjects = associated.get(matches.get(0));
                boolean alreadyPresent = recordProjects.stream().anyMatch(existing ->
                        sameText(value(existing.projectName()), value(project.projectName())));
                if (!alreadyPresent) {
                    recordProjects.add(project);
                }
            } else if (matches.size() > 1) {
                warnings.add("Project employment association is ambiguous for " + value(project.projectName()));
            }
        }
        List<ParsedEmployment> result = new ArrayList<>();
        for (int index = 0; index < employment.size(); index++) {
            ParsedEmployment record = employment.get(index);
            result.add(new ParsedEmployment(record.company(), record.position(), record.startDate(), record.endDate(),
                    record.location(), record.employmentType(), record.industry(), associated.get(index)));
        }
        return List.copyOf(result);
    }

    private static void validateDateRanges(
            List<ParsedEmployment> employment,
            List<ParsedProject> projects,
            List<ParsedEducation> education,
            List<String> warnings
    ) {
        for (int index = 0; index < employment.size(); index++) {
            if (!validRange(employment.get(index).startDate(), employment.get(index).endDate())) {
                warnings.add("Employment date range is invalid at index " + index);
            }
        }
        for (int index = 0; index < projects.size(); index++) {
            if (!validRange(projects.get(index).startDate(), projects.get(index).endDate())) {
                warnings.add("Project date range is invalid at index " + index);
            }
        }
        for (int index = 0; index < education.size(); index++) {
            if (!validRange(education.get(index).startDate(), education.get(index).endDate())) {
                warnings.add("Education date range is invalid at index " + index);
            }
        }
    }

    private static boolean validRange(ExtractedValue<String> start, ExtractedValue<String> end) {
        LocalDate startDate = parseDate(start == null ? null : start.value(), false, LocalDate.now());
        LocalDate endDate = parseDate(end == null ? null : end.value(), true, LocalDate.now());
        return (start == null || startDate != null) && (end == null || endDate != null)
                && (startDate == null || endDate == null || !endDate.isBefore(startDate));
    }

    private static List<ParsedSkill> mergeSkills(
            List<ParsedSkill> extracted,
            List<ParsedEmployment> employment,
            List<ParsedProject> projects
    ) {
        Map<String, SkillBuilder> merged = new LinkedHashMap<>();
        extracted.forEach(skill -> addSkill(merged, skill.name(), skill.group(), skill.evidence(),
            skill.canonicalName()));
        for (ParsedProject project : allProjects(employment, projects)) {
            for (ExtractedValue<String> technology : project.technologiesAndTools()) {
                addSkill(merged, technology, null, List.of(technology), null);
            }
            for (String text : projectText(project)) {
                for (SkillCatalog.Mention mention : SkillCatalog.mentions(text)) {
                    ExtractedValue<String> evidence = new ExtractedValue<>(mention.matchedText(), 0.85, text);
                    addSkill(merged, new ExtractedValue<>(mention.matchedText(), 0.85, text), null,
                            List.of(evidence), null);
                }
            }
        }
        return merged.values().stream().map(builder -> builder.toParsedSkill()).toList();
    }

    private static void addSkill(
            Map<String, SkillBuilder> merged,
            ExtractedValue<String> name,
            ExtractedValue<String> group,
                List<ExtractedValue<String>> evidence,
                String canonicalSuggestion
    ) {
        if (name == null || name.value().isBlank()) {
            return;
        }
        SkillCatalog.Entry exactMatch = SkillCatalog.find(name.value()).orElse(null);
        SkillCatalog.Entry suggestedMatch = exactMatch == null && canonicalSuggestion != null
            ? SkillCatalog.find(canonicalSuggestion)
                .filter(entry -> entry.canonicalName().equalsIgnoreCase(canonicalSuggestion.strip()))
                .orElse(null)
            : null;
        SkillCatalog.Entry catalog = exactMatch == null ? suggestedMatch : exactMatch;
        boolean aiSuggested = exactMatch == null && suggestedMatch != null;
        String key = catalog == null ? normalizeKey(name.value()) : catalog.id().toString();
        SkillBuilder builder = merged.computeIfAbsent(key,
            ignored -> new SkillBuilder(name, group, catalog, aiSuggested));
        if (exactMatch != null && builder.aiSuggested) {
            builder.aiSuggested = false;
        }
        if (builder.group == null && group != null) {
            builder.group = group;
        }
        evidence.stream().filter(Objects::nonNull).forEach(builder::addEvidence);
        if (name.sourceText() != null) {
            builder.addEvidence(name);
        }
    }

    private static ParsedSkill calculateExperience(
            ParsedSkill skill,
            List<ParsedEmployment> employment,
            List<ParsedProject> projects,
            LocalDate today
    ) {
        SkillCatalog.Entry catalog = SkillCatalog.find(value(skill.name())).orElseGet(() -> {
            String suggestion = skill.canonicalName();
            if (suggestion == null) {
                return null;
            }
            return SkillCatalog.find(suggestion)
                    .filter(entry -> entry.canonicalName().equalsIgnoreCase(suggestion.strip()))
                    .orElse(null);
        });
        List<Period> periods = new ArrayList<>();
        for (ParsedEmployment record : employment) {
            for (ParsedProject project : record.projects()) {
                if (mentionsSkill(skill, project)) {
                    Period period = period(project, record, today);
                    if (period != null) {
                        periods.add(period);
                    }
                }
            }
            if (mentionsSkill(skill, record)) {
                Period period = period(record, today);
                if (period != null) {
                    periods.add(period);
                }
            }
        }
        for (ParsedProject project : projects) {
            if (mentionsSkill(skill, project)) {
                Period period = period(project, null, today);
                if (period != null) {
                    periods.add(period);
                }
            }
        }
        List<Period> merged = mergePeriods(periods);
        long days = merged.stream().mapToLong(period -> ChronoUnit.DAYS.between(period.start(), period.end()) + 1).sum();
        BigDecimal years = days == 0 ? null : BigDecimal.valueOf(days)
                .divide(BigDecimal.valueOf(365.2425), 2, RoundingMode.HALF_UP);
        LocalDate lastUsed = merged.stream().map(Period::end).max(Comparator.naturalOrder()).orElse(null);
        return new ParsedSkill(skill.name(), skill.group(), skill.evidence(),
                catalog == null ? null : catalog.id(), catalog == null ? null : catalog.canonicalName(), years,
            lastUsed, catalog == null || skill.requiresReview());
    }

    private static boolean mentionsSkill(ParsedSkill skill, ParsedProject project) {
        String name = value(skill.name());
        if (name == null) {
            return false;
        }
        if (project.technologiesAndTools().stream().anyMatch(technology -> sameText(value(technology), name))) {
            return true;
        }
        String text = String.join(" ", projectText(project));
        if (SkillCatalog.find(name).isPresent()) {
            return SkillCatalog.mentions(text).stream()
                    .anyMatch(mention -> mention.entry().id().equals(SkillCatalog.find(name).orElseThrow().id()));
        }
        return containsWord(text, name);
    }

    private static boolean mentionsSkill(ParsedSkill skill, ParsedEmployment employment) {
        String text = String.join(" ", java.util.stream.Stream.of(value(employment.company()),
            value(employment.position()), value(employment.location()), value(employment.employmentType()),
            value(employment.industry())).filter(Objects::nonNull).toList());
        String name = value(skill.name());
        if (name == null) {
            return false;
        }
        if (SkillCatalog.find(name).isPresent()) {
            UUID catalogId = SkillCatalog.find(name).orElseThrow().id();
            return SkillCatalog.mentions(text).stream().anyMatch(mention -> mention.entry().id().equals(catalogId));
        }
        return containsWord(text, name);
    }

    private static Period period(ParsedEmployment employment, LocalDate today) {
        LocalDate start = parseDate(value(employment.startDate()), false, today);
        String endValue = value(employment.endDate());
        LocalDate end = isCurrent(endValue) ? today : parseDate(endValue, true, today);
        return start == null || end == null || end.isBefore(start) ? null : new Period(start, end);
    }

    private static Period period(ParsedProject project, ParsedEmployment employment, LocalDate today) {
        String startValue = value(project.startDate());
        String endValue = value(project.endDate());
        if (startValue == null && employment != null) {
            startValue = value(employment.startDate());
        }
        if (endValue == null && employment != null) {
            endValue = value(employment.endDate());
        }
        LocalDate start = parseDate(startValue, false, today);
        boolean current = isCurrent(endValue);
        LocalDate end = current ? today : parseDate(endValue, true, today);
        return start == null || end == null || end.isBefore(start) ? null : new Period(start, end);
    }

    private static List<Period> mergePeriods(List<Period> periods) {
        List<Period> sorted = periods.stream().distinct().sorted(Comparator.comparing(Period::start)).toList();
        List<Period> merged = new ArrayList<>();
        for (Period period : sorted) {
            if (merged.isEmpty()) {
                merged.add(period);
                continue;
            }
            Period previous = merged.get(merged.size() - 1);
            if (!period.start().isAfter(previous.end().plusDays(1))) {
                merged.set(merged.size() - 1, new Period(previous.start(),
                        previous.end().isAfter(period.end()) ? previous.end() : period.end()));
            } else {
                merged.add(period);
            }
        }
        return merged;
    }

    private static ParsedLanguage normalizeLanguage(ParsedLanguage language, String sourceText, List<String> warnings) {
        ExtractedValue<String> name = language.name();
        ExtractedValue<String> proficiency = language.proficiency();
        if (proficiency != null) {
            java.util.regex.Matcher level = CEFR_LEVEL.matcher(proficiency.value());
            if (level.find() && !containsWord(sourceText, level.group(1))) {
                warnings.add("Language proficiency requires review because its CEFR level is not present in source text");
                proficiency = null;
            }
        }
        if (name == null) {
            return new ParsedLanguage(null, proficiency);
        }
        String rawName = name.value().strip();
        String normalized = LANGUAGE_NAMES.get(rawName.toLowerCase(Locale.ROOT));
        if (normalized == null && rawName.matches("(?i)[a-z]{2,3}")) {
            String displayName = Locale.forLanguageTag(rawName).getDisplayLanguage(Locale.ENGLISH);
            if (!displayName.isBlank() && !displayName.equalsIgnoreCase(rawName)) {
                normalized = displayName;
            }
        }
        return normalized == null ? language : new ParsedLanguage(
            new ExtractedValue<>(normalized, name.confidence(), name.sourceText()), proficiency);
    }

    private static List<ParsedProject> allProjects(List<ParsedEmployment> employment, List<ParsedProject> projects) {
        Map<String, ParsedProject> result = new LinkedHashMap<>();
        projects.forEach(project -> result.put(projectKey(project), project));
        employment.stream().flatMap(record -> record.projects().stream())
                .forEach(project -> result.putIfAbsent(projectKey(project), project));
        return List.copyOf(result.values());
    }

    private static List<String> projectText(ParsedProject project) {
        List<String> values = new ArrayList<>();
        addValue(values, project.projectName());
        addValue(values, project.projectDescription());
        project.responsibilities().forEach(value -> addValue(values, value));
        project.technologiesAndTools().forEach(value -> addValue(values, value));
        return values;
    }

    private static void addValue(List<String> values, ExtractedValue<String> value) {
        if (value != null && value.value() != null) {
            values.add(value.value());
        }
    }

    private static String projectKey(ParsedProject project) {
        String name = normalizeKey(value(project.projectName()));
        return name.isBlank() ? Integer.toHexString(project.hashCode()) : name;
    }

    private static boolean containsWord(String text, String value) {
        return Pattern.compile("(?i)(?<![\\p{Alnum}])" + Pattern.quote(value) + "(?![\\p{Alnum}])")
                .matcher(text).find();
    }

    private static boolean sameText(String left, String right) {
        return left != null && right != null && left.strip().equalsIgnoreCase(right.strip());
    }

    private static String normalizeKey(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String value(ExtractedValue<String> value) {
        return value == null ? null : value.value();
    }

    private static boolean isCurrent(String value) {
        return value != null && Set.of("present", "current", "ongoing", "now").contains(value.strip().toLowerCase(Locale.ROOT));
    }

    private static LocalDate parseDate(String value, boolean endDate, LocalDate today) {
        if (value == null || isCurrent(value)) {
            return null;
        }
        try {
            if (YEAR.matcher(value).matches()) {
                return endDate ? LocalDate.of(Integer.parseInt(value), 12, 31)
                        : LocalDate.of(Integer.parseInt(value), 1, 1);
            }
            if (YEAR_MONTH.matcher(value).matches()) {
                YearMonth month = YearMonth.parse(value);
                return endDate ? month.atEndOfMonth() : month.atDay(1);
            }
            return FULL_DATE.matcher(value).matches() ? LocalDate.parse(value) : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private record Period(LocalDate start, LocalDate end) {
    }

    private static final class SkillBuilder {

        private ExtractedValue<String> name;
        private ExtractedValue<String> group;
        private final SkillCatalog.Entry catalog;
        private boolean aiSuggested;
        private final Map<String, ExtractedValue<String>> evidence = new LinkedHashMap<>();

        private SkillBuilder(ExtractedValue<String> name, ExtractedValue<String> group,
                SkillCatalog.Entry catalog, boolean aiSuggested) {
            this.name = name;
            this.group = group;
            this.catalog = catalog;
            this.aiSuggested = aiSuggested;
        }

        private void addEvidence(ExtractedValue<String> value) {
            String key = normalizeKey(value.value()) + "|" + normalizeKey(value.sourceText());
            evidence.putIfAbsent(key, value);
        }

        private ParsedSkill toParsedSkill() {
            return new ParsedSkill(name, group, List.copyOf(evidence.values()),
                    catalog == null ? null : catalog.id(), catalog == null ? null : catalog.canonicalName(),
                    null, null, catalog == null || aiSuggested);
        }
    }
}