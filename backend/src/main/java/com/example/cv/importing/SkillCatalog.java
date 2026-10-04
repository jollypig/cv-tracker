package com.example.cv.importing;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SkillCatalog {

    private static final List<Entry> ENTRIES = List.of(
            entry("Java", "J2SE"),
            entry("JavaScript", "JS", "ECMAScript"),
            entry("TypeScript", "TS"),
            entry("Python", "Python 3"),
            entry("C#", "C Sharp", "CSharp"),
            entry("C++", "Cpp", "C Plus Plus"),
            entry(".NET", "Dotnet", "NET"),
            entry("Spring Framework", "Spring"),
            entry("Spring Boot"),
            entry("PostgreSQL", "Postgres", "Postgre"),
            entry("MySQL"),
            entry("Oracle Database", "Oracle DB"),
            entry("SQL", "Structured Query Language"),
            entry("HTML", "HTML5"),
            entry("CSS", "CSS3"),
            entry("React", "React.js", "ReactJS"),
            entry("Vue.js", "Vue", "VueJS"),
            entry("Node.js", "Node", "NodeJS"),
            entry("Docker"),
            entry("Kubernetes", "K8s"),
            entry("Amazon Web Services", "AWS"),
            entry("Microsoft Azure", "Azure"),
            entry("Git"),
            entry("Linux")
    );
    private static final Map<String, Entry> BY_ALIAS = buildAliasIndex();

    private SkillCatalog() {
    }

    static Optional<Entry> find(String name) {
        return Optional.ofNullable(BY_ALIAS.get(normalize(name)));
    }

    static List<String> canonicalNames() {
        return ENTRIES.stream().map(Entry::canonicalName).toList();
    }

    static List<Mention> mentions(String text) {
        List<Candidate> candidates = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            for (String alias : entry.aliases()) {
                Matcher matcher = Pattern.compile("(?i)(?<![\\p{Alnum}])" + Pattern.quote(alias)
                        + "(?![\\p{Alnum}])").matcher(text);
                while (matcher.find()) {
                    candidates.add(new Candidate(entry, matcher.group(), matcher.start(), matcher.end()));
                }
            }
        }
        candidates.sort(Comparator.comparingInt(Candidate::start)
                .thenComparing(Comparator.comparingInt((Candidate candidate) -> candidate.end() - candidate.start())
                        .reversed()));
        Map<UUID, Mention> matches = new LinkedHashMap<>();
        List<Candidate> selected = new ArrayList<>();
        for (Candidate candidate : candidates) {
            boolean overlaps = selected.stream().anyMatch(existing -> candidate.start() < existing.end()
                    && existing.start() < candidate.end());
            if (!overlaps && !matches.containsKey(candidate.entry().id())) {
                matches.put(candidate.entry().id(), new Mention(candidate.entry(), candidate.matchedText()));
                selected.add(candidate);
            }
        }
        return List.copyOf(matches.values());
    }

    private static Entry entry(String canonicalName, String... aliases) {
        List<String> names = new ArrayList<>();
        names.add(canonicalName);
        names.addAll(List.of(aliases));
        UUID id = UUID.nameUUIDFromBytes(("cv-tracker:skill:" + canonicalName).getBytes(StandardCharsets.UTF_8));
        return new Entry(id, canonicalName, List.copyOf(names));
    }

    private static Map<String, Entry> buildAliasIndex() {
        Map<String, Entry> aliases = new LinkedHashMap<>();
        for (Entry entry : ENTRIES) {
            for (String alias : entry.aliases()) {
                aliases.put(normalize(alias), entry);
            }
        }
        return Map.copyOf(aliases);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    record Entry(UUID id, String canonicalName, List<String> aliases) {
    }

    record Mention(Entry entry, String matchedText) {
    }

    private record Candidate(Entry entry, String matchedText, int start, int end) {
    }
}