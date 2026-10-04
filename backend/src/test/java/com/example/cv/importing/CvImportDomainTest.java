package com.example.cv.importing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CvImportDomainTest {

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
    void rejectsConfidenceOutsideZeroToOne() {
        assertThatThrownBy(() -> new ExtractedValue<>("value", 1.1, "source"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsLinkBlocksWithoutTarget() {
        assertThatThrownBy(() -> new NormalizedCvBlock(NormalizedCvBlock.Type.LINK, "Portfolio", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}