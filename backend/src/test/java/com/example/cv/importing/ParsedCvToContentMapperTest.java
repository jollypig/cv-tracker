package com.example.cv.importing;

import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParsedCvToContentMapperTest {

    private final ParsedCvToContentMapper mapper = new ParsedCvToContentMapper(
            Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void mapsReviewedSectionsIntoCvContent() {
        ParsedProject nestedProject = new ParsedProject(value("Acme"), List.of(value("Finance")), value("Ledger"),
                value("Built reporting"), value("2022-01"), value("Present"), value("Lead"),
                List.of(value("Designed APIs")), List.of(value("Java"), value("PostgreSQL")));
        ParsedCv draft = new ParsedCv(null, value("Backend engineer"),
                List.of(new ParsedEmployment(value("Acme"), value("Engineer"), value("2020"), value("Present"),
                        value("Riga"), value("Full-time"), null, List.of(nestedProject))),
                List.of(),
                List.of(new ParsedEducation(value("University"), value("BSc"), value("Computer Science"),
                        value("2015"), value("2019"), null)),
                List.of(new ParsedLanguage(value("English"), value("Fluent"))),
                List.of(new ParsedSkill(value("Java"), value("Backend"), List.of(value("Built services")))));

        var content = mapper.toContent(draft);

        assertThat(content.summary()).isEqualTo("Backend engineer");
        assertThat(content.experiences()).singleElement().satisfies(experience -> {
            assertThat(experience.company()).isEqualTo("Acme");
            assertThat(experience.startDate()).hasToString("2020-01-01");
            assertThat(experience.current()).isTrue();
            assertThat(experience.projects()).singleElement().satisfies(project -> {
                assertThat(project.projectName()).isEqualTo("Ledger");
                assertThat(project.responsibilities()).isEqualTo("Designed APIs");
                assertThat(project.technologies()).isEqualTo("Java, PostgreSQL");
            });
        });
        assertThat(content.education()).singleElement().satisfies(item ->
                assertThat(item.institution()).isEqualTo("University"));
        assertThat(content.languages()).singleElement().satisfies(language ->
                assertThat(language.level()).isEqualTo("Fluent"));
        assertThat(content.skillGroups()).singleElement().satisfies(group -> {
            assertThat(group.name()).isEqualTo("Backend");
            assertThat(group.skills()).singleElement().satisfies(skill -> assertThat(skill.name()).isEqualTo("Java"));
        });
    }

    @Test
    void rejectsApprovalWhenRequiredDraftFieldsAreMissing() {
        ParsedCv draft = new ParsedCv(null, null,
                List.of(new ParsedEmployment(null, value("Engineer"), null, null, null, null, null, List.of())),
                List.of(), List.of(), List.of(), List.of());

        assertThatThrownBy(() -> mapper.toContent(draft))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("missing employment company");
    }

    private static ExtractedValue<String> value(String value) {
        return new ExtractedValue<>(value, 0.9, "source");
    }
}