package com.example.cv.importing;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CvImportDomainTest {

        private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsedCvRoundTripsThroughJsonWithEvidenceAndPartialDates() throws Exception {
        ParsedCv parsedCv = new ParsedCv(
                new PersonalData(
                        new ExtractedValue<>("Ada", 0.99, "Ada Lovelace"),
                        new ExtractedValue<>("Lovelace", 0.99, "Ada Lovelace"),
                        new ExtractedValue<>("ada@example.com", 1.0, "ada@example.com"),
                        null,
                        null,
                        List.of(new ExtractedValue<>("https://example.com", 0.95, "Portfolio"))
                ),
                new ExtractedValue<>("Mathematician", 0.8, "Mathematician"),
                List.of(new ParsedEmployment(
                        new ExtractedValue<>("Analytical Engines Ltd", 0.9, "Analytical Engines Ltd"),
                        new ExtractedValue<>("Programmer", 0.9, "Programmer"),
                        new ExtractedValue<>("1842", 0.7, "1842"),
                        null,
                        null,
                        null,
                        null,
                        List.of()
                )),
                List.of(new ParsedProject(
                        null,
                        List.of(),
                        new ExtractedValue<>("Analytical Engine", 0.9, "Analytical Engine"),
                        null,
                        null,
                        null,
                        null,
                        List.of(new ExtractedValue<>("Published notes", 0.8, "Published notes")),
                        List.of(new ExtractedValue<>("Babbage's engine", 0.7, "Babbage's engine"))
                )),
                List.of(),
                List.of(new ParsedLanguage(new ExtractedValue<>("English", 0.9, "English"), null)),
                List.of(new ParsedSkill(
                        new ExtractedValue<>("Mathematics", 0.9, "Mathematical work"),
                        new ExtractedValue<>("Science", 0.7, "Science"),
                        List.of(new ExtractedValue<>("Mathematical work", 0.9, "Mathematical work"))
                ))
        );

        String json = objectMapper.writeValueAsString(parsedCv);

        assertThat(objectMapper.readValue(json, ParsedCv.class)).isEqualTo(parsedCv);
    }

    @Test
    void normalizedDocumentRetainsPageBoundariesAndLinksThroughJson() throws Exception {
        NormalizedCvDocument document = new NormalizedCvDocument(List.of(
                new NormalizedCvBlock(NormalizedCvBlock.Type.HEADING, "Experience", 1),
                new NormalizedCvBlock(NormalizedCvBlock.Type.LINK, "Portfolio", 2, "https://example.com")
        ));

        assertThat(objectMapper.readValue(objectMapper.writeValueAsString(document), NormalizedCvDocument.class))
                .isEqualTo(document);
    }

    @Test
    void normalizedDocumentCleansTextAndPreservesBlockMetadataAndOrder() {
        NormalizedCvDocument document = new NormalizedCvDocument(List.of(
                new NormalizedCvBlock(NormalizedCvBlock.Type.TEXT,
                        "\uFEFF  Senior   Engineer\r\n\r\n \tRemote\u00A0work\u0000 ", 2),
                new NormalizedCvBlock(NormalizedCvBlock.Type.TEXT, "\u200B", 3),
                new NormalizedCvBlock(NormalizedCvBlock.Type.LINK, " Portfolio\tsite ", 4, "https://example.com")
        ));

        assertThat(document.blocks())
                .extracting(NormalizedCvBlock::text)
                .containsExactly("Senior Engineer\n\nRemote work", "Portfolio site");
        assertThat(document.blocks())
                .extracting(NormalizedCvBlock::pageNumber)
                .containsExactly(2, 4);
        assertThat(document.blocks().get(1).link()).isEqualTo("https://example.com");
    }

    @Test
        void validatesConfidenceOutsideZeroToOne() {
                assertThat(VALIDATOR.validate(new ExtractedValue<>("value", 1.1, "source")))
                                .extracting(violation -> violation.getPropertyPath().toString())
                                .containsExactly("confidence");
    }

    @Test
    void rejectsLinkBlocksWithoutTarget() {
        assertThatThrownBy(() -> new NormalizedCvBlock(NormalizedCvBlock.Type.LINK, "Portfolio", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalizesSkillAliasesMergesEvidenceAndCalculatesOverlappingPeriodsOnce() {
        ParsedProject firstProject = new ParsedProject(
                new ExtractedValue<>("Acme", 1.0, "Acme"), List.of(),
                new ExtractedValue<>("Atlas", 1.0, "Atlas"), null,
                new ExtractedValue<>("2020-01-01", 1.0, "2020-01-01"),
                new ExtractedValue<>("2021-12-31", 1.0, "2021-12-31"), null, List.of(),
                List.of(new ExtractedValue<>("Java", 0.9, "Java"),
                        new ExtractedValue<>("Spring", 0.9, "Spring")));
        ParsedProject secondProject = new ParsedProject(
                new ExtractedValue<>("Acme", 1.0, "Acme"), List.of(),
                new ExtractedValue<>("Orion", 1.0, "Orion"), null,
                new ExtractedValue<>("2021-01-01", 1.0, "2021-01-01"),
                new ExtractedValue<>("2022-12-31", 1.0, "2022-12-31"), null, List.of(),
                List.of(new ExtractedValue<>("Java", 0.9, "Java")));
        ParsedEmployment employment = new ParsedEmployment(
                new ExtractedValue<>("Acme", 1.0, "Acme"), null,
                new ExtractedValue<>("2020-01-01", 1.0, "2020-01-01"),
                new ExtractedValue<>("2022-12-31", 1.0, "2022-12-31"), null, null, null, List.of());
        ParsedSkill rawAlias = new ParsedSkill(new ExtractedValue<>("J2SE", 0.9, "J2SE"), null,
                List.of(new ExtractedValue<>("Java", 0.8, "Java")));
        ParsedSkill unmatched = new ParsedSkill(new ExtractedValue<>("Made-up Platform", 0.8, "Made-up Platform"),
                null, List.of());
        ParsedCv input = new ParsedCv(null, null, List.of(employment), List.of(firstProject, secondProject), List.of(),
                List.of(new ParsedLanguage(new ExtractedValue<>("de", 0.9, "Deutsch"),
                        new ExtractedValue<>("B2", 0.9, "B2"))), List.of(rawAlias, unmatched));

        ParsedCv result = ParsedCvNormalizer.normalize(input, LocalDate.of(2023, 1, 1), "Deutsch B2");

        assertThat(result.skills()).hasSize(3);
        ParsedSkill java = result.skills().stream().filter(skill -> "Java".equals(skill.canonicalName())).findFirst()
                .orElseThrow();
        assertThat(java.canonicalSkillId()).isNotNull();
        assertThat(java.yearsOfExperience()).isEqualByComparingTo(new BigDecimal("3.00"));
        assertThat(java.lastUsedDate()).isEqualTo(LocalDate.of(2022, 12, 31));
        ParsedSkill spring = result.skills().stream().filter(skill -> "Spring Framework".equals(skill.canonicalName()))
                .findFirst().orElseThrow();
        assertThat(spring.name().value()).isEqualTo("Spring");
        assertThat(result.skills().stream().filter(skill -> "Made-up Platform".equals(skill.name().value()))
                .findFirst().orElseThrow().requiresReview()).isTrue();
        assertThat(result.languages().get(0).name().value()).isEqualTo("German");
        assertThat(result.languages().get(0).proficiency().value()).isEqualTo("B2");
        assertThat(result.employment().get(0).projects()).containsExactly(firstProject, secondProject);
    }

        @Test
        void keepsOnlyCefrLevelsPresentInSourceText() {
                ParsedCv input = new ParsedCv(null, null, List.of(), List.of(), List.of(), List.of(
                                new ParsedLanguage(new ExtractedValue<>("English", 0.9, "English"),
                                                new ExtractedValue<>("C1", 0.9, "C1")),
                                new ParsedLanguage(new ExtractedValue<>("German", 0.9, "German"),
                                                new ExtractedValue<>("B2", 0.9, "Upper intermediate"))), List.of());

                ParsedCv result = ParsedCvNormalizer.normalize(input, LocalDate.of(2025, 1, 1), "English C1; German upper intermediate");

                assertThat(result.languages().get(0).proficiency().value()).isEqualTo("C1");
                assertThat(result.languages().get(1).proficiency()).isNull();
                assertThat(result.warnings()).anyMatch(warning -> warning.contains("CEFR level is not present"));
        }

    @Test
    void flagsAmbiguousAssociationsAndInvalidRangesButAllowsIncompleteDates() {
        ParsedEmployment invalidEmployment = new ParsedEmployment(
                new ExtractedValue<>("Acme", 1.0, "Acme"), null,
                new ExtractedValue<>("2024", 0.8, "2024"), new ExtractedValue<>("2020", 0.8, "2020"),
                null, null, null, List.of());
        ParsedEmployment incompleteEmployment = new ParsedEmployment(
                new ExtractedValue<>("Beta", 1.0, "Beta"), null,
                new ExtractedValue<>("2021", 0.8, "2021"), null, null, null, null, List.of());
        ParsedProject ambiguousProject = new ParsedProject(null, List.of(),
                new ExtractedValue<>("Unassigned", 0.8, "Unassigned"), null, null, null, null, List.of(), List.of());
        ParsedCv input = new ParsedCv(null, null, List.of(invalidEmployment, incompleteEmployment),
                List.of(ambiguousProject), List.of(), List.of(), List.of());

        ParsedCv result = ParsedCvNormalizer.normalize(input, LocalDate.of(2025, 1, 1));

        assertThat(result.warnings()).anyMatch(warning -> warning.contains("date range is invalid"));
        assertThat(result.warnings()).anyMatch(warning -> warning.contains("association is ambiguous"));
        assertThat(result.employment().get(1).startDate().value()).isEqualTo("2021");
        assertThat(result.employment().get(1).endDate()).isNull();
    }

        @Test
        void catalogPrefersLongerCompoundSkillAliases() {
                assertThat(SkillCatalog.mentions("Built a service with Spring Boot"))
                                .extracting(mention -> mention.entry().canonicalName())
                                .containsExactly("Spring Boot");
        }

            @Test
            void acceptsOnlyAllowlistedAiSkillSuggestionsAndRebuildsCanonicalIds() {
                ParsedSkill suggested = new ParsedSkill(new ExtractedValue<>("Postgres-compatible", 0.7,
                        "Postgres-compatible"), null, List.of(), java.util.UUID.randomUUID(), "PostgreSQL",
                        new BigDecimal("99"), LocalDate.of(2099, 1, 1), false);
                ParsedSkill invalidSuggestion = new ParsedSkill(new ExtractedValue<>("Unknown Engine", 0.8,
                        "Unknown Engine"), null, List.of(), java.util.UUID.randomUUID(), "Invented Canonical Skill",
                        new BigDecimal("50"), LocalDate.of(2099, 1, 1), false);
                ParsedCv input = new ParsedCv(null, null, List.of(), List.of(), List.of(), List.of(),
                        List.of(suggested, invalidSuggestion));

                ParsedCv result = ParsedCvNormalizer.normalize(input, LocalDate.of(2025, 1, 1));

                ParsedSkill mapped = result.skills().stream().filter(skill -> "Postgres-compatible".equals(skill.name().value()))
                        .findFirst().orElseThrow();
                assertThat(mapped.canonicalName()).isEqualTo("PostgreSQL");
                assertThat(mapped.canonicalSkillId()).isEqualTo(SkillCatalog.find("PostgreSQL").orElseThrow().id());
                assertThat(mapped.requiresReview()).isTrue();
                assertThat(mapped.yearsOfExperience()).isNull();
                assertThat(mapped.lastUsedDate()).isNull();
                ParsedSkill unmatched = result.skills().stream().filter(skill -> "Unknown Engine".equals(skill.name().value()))
                        .findFirst().orElseThrow();
                assertThat(unmatched.canonicalSkillId()).isNull();
                assertThat(unmatched.canonicalName()).isNull();
                assertThat(unmatched.requiresReview()).isTrue();
            }
}