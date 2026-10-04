package com.example.cv.importing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CvPersonalDataEnricher {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\w)(?:\\+?\\d[\\d\\h()./-]{5,}\\d)(?!\\w)");
    private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b(?:https?://|www\\.)[^\\s<>()]+");

    private CvPersonalDataEnricher() {
    }

    static ParsedCv enrich(ParsedCv parsedCv, NormalizedCvDocument document) {
        String text = document.blocks().stream()
                .map(NormalizedCvBlock::text)
                .collect(java.util.stream.Collectors.joining("\n"));
        PersonalData existing = parsedCv.personalData();
        if (existing == null) {
            existing = new PersonalData(null, null, null, null, null, List.of());
        }

        ExtractedValue<String> email = firstMatch(EMAIL_PATTERN, text, 1.0);
        if (email == null) {
            email = supported(existing.email(), text);
        }
        ExtractedValue<String> phone = firstPhone(text);
        if (phone == null) {
            phone = supported(existing.phone(), text);
        }

        Map<String, ExtractedValue<String>> urls = new LinkedHashMap<>();
        Matcher urlMatcher = URL_PATTERN.matcher(text);
        while (urlMatcher.find()) {
            String value = trimUrlPunctuation(urlMatcher.group());
            addUrl(urls, new ExtractedValue<>(value, 1.0, value));
        }
        for (NormalizedCvBlock block : document.blocks()) {
            if (block.link() != null) {
                addUrl(urls, new ExtractedValue<>(block.link(), 1.0, block.text()));
            }
        }
        existing.urls().stream()
                .map(value -> supported(value, text))
                .filter(java.util.Objects::nonNull)
                .forEach(value -> addUrl(urls, value));

        ExtractedValue<String> firstName = supported(existing.firstName(), text);
        ExtractedValue<String> lastName = supported(existing.lastName(), text);
        ExtractedValue<String> location = supported(existing.location(), text);
        List<String> warnings = new java.util.ArrayList<>(parsedCv.warnings());
        addReviewWarning(warnings, "first name", existing.firstName(), firstName);
        addReviewWarning(warnings, "last name", existing.lastName(), lastName);
        addReviewWarning(warnings, "location", existing.location(), location);
        addReviewWarning(warnings, "email", existing.email(), email);
        addReviewWarning(warnings, "phone", existing.phone(), phone);
        for (ExtractedValue<String> url : existing.urls()) {
            if (supported(url, text) == null && !urls.containsKey(url.value().toLowerCase(Locale.ROOT))) {
            warnings.add("Personal URL requires review because it is not supported by source text");
            }
        }

        PersonalData enriched = new PersonalData(
            firstName,
            lastName,
                email,
                phone,
            location,
                List.copyOf(urls.values())
        );
        ParsedCv enrichedCv = new ParsedCv(enriched, parsedCv.professionalSummary(), parsedCv.employment(),
            parsedCv.projects(), parsedCv.education(), parsedCv.languages(), parsedCv.skills(), warnings);
        return ParsedCvNormalizer.normalize(enrichedCv, java.time.LocalDate.now(), text);
    }

    private static ExtractedValue<String> firstMatch(Pattern pattern, String text, double confidence) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? new ExtractedValue<>(matcher.group(), confidence, matcher.group()) : null;
    }

    private static ExtractedValue<String> firstPhone(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        while (matcher.find()) {
            String value = matcher.group().trim();
            long digits = value.chars().filter(Character::isDigit).count();
            if (digits >= 7 && digits <= 15 && !looksLikeDateRange(value)) {
                return new ExtractedValue<>(value, 0.95, value);
            }
        }
        return null;
    }

    private static ExtractedValue<String> supported(ExtractedValue<String> value, String text) {
        if (value == null || value.value().isBlank()) {
            return null;
        }
        String source = value.sourceText();
        if (!containsIgnoreCase(text, value.value())
                || (source != null && !containsIgnoreCase(text, source))) {
            return null;
        }
        return value;
    }

    private static boolean containsIgnoreCase(String text, String value) {
        return text.toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT));
    }

    private static String trimUrlPunctuation(String value) {
        int end = value.length();
        while (end > 0 && ".,;:!?".indexOf(value.charAt(end - 1)) >= 0) {
            end--;
        }
        return value.substring(0, end);
    }

    private static void addUrl(Map<String, ExtractedValue<String>> urls, ExtractedValue<String> url) {
        urls.putIfAbsent(url.value().toLowerCase(Locale.ROOT), url);
    }

    private static boolean looksLikeDateRange(String value) {
        return value.matches("\\d{4}[-/]\\d{1,2}[-/]\\d{1,4}")
                || value.matches("\\d{4}\\h*[-/]\\h*\\d{4}");
    }

    private static void addReviewWarning(
            List<String> warnings,
            String field,
            ExtractedValue<String> suggested,
            ExtractedValue<String> supported
    ) {
        if (suggested != null && (supported == null
            || (suggested.confidence() != null && suggested.confidence() < 0.7))) {
            warnings.add("Personal " + field + " requires review because it is not supported by source text");
        }
    }
}