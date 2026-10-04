package com.example.cv.importing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CvParserCorpusTest {

    private record CorpusFixture(
            String id,
            String html,
            String pdf,
            Map<String, List<String>> expected,
            Map<String, List<String>> pdfExpected) {
    }

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HtmlCvParser htmlParser = new HtmlCvParser(10 * 1024 * 1024);
    private final PdfCvParser pdfParser = new PdfCvParser(10 * 1024 * 1024);

    @TestFactory
    Stream<DynamicTest> parserCorpusMatchesExpectedFieldsByCategory() throws IOException {
        List<CorpusFixture> corpus = objectMapper.readValue(
                getClass().getResourceAsStream("/cv-parser-corpus.json"), new TypeReference<>() {
                });
        ArrayList<DynamicTest> tests = new ArrayList<>();

        for (CorpusFixture fixture : corpus) {
            addHtmlTests(tests, fixture);
            addPdfTests(tests, fixture);
        }

        assertThat(corpus).hasSize(20);
        return tests.stream();
    }

    private void addHtmlTests(ArrayList<DynamicTest> tests, CorpusFixture fixture) {
        for (Map.Entry<String, List<String>> field : fixture.expected().entrySet()) {
            for (String expectedText : field.getValue()) {
                tests.add(DynamicTest.dynamicTest(fixture.id() + " / HTML / " + field.getKey() + " / " + expectedText,
                        () -> assertContains(htmlParser.parse(fixture.html().getBytes(StandardCharsets.UTF_8),
                                "text/html"), expectedText)));
            }
        }
    }

    private void addPdfTests(ArrayList<DynamicTest> tests, CorpusFixture fixture) {
        Map<String, List<String>> expectedFields = fixture.pdfExpected() == null
                ? fixture.expected()
                : fixture.pdfExpected();
        for (Map.Entry<String, List<String>> field : expectedFields.entrySet()) {
            for (String expectedText : field.getValue()) {
                tests.add(DynamicTest.dynamicTest(fixture.id() + " / PDF / " + field.getKey() + " / " + expectedText,
                        () -> assertContains(pdfParser.parse(createPdf(fixture.pdf()), "application/pdf"), expectedText)));
            }
        }
    }

    private void assertContains(NormalizedCvDocument document, String expected) {
        String normalizedText = document.blocks().stream()
                .map(NormalizedCvBlock::text)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("")
                .toLowerCase(Locale.ROOT);
        assertThat(normalizedText).contains(expected.toLowerCase(Locale.ROOT));
    }

    private byte[] createPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(48, 750);
                for (String line : text.split("\\R")) {
                    content.showText(line);
                    content.newLineAtOffset(0, -14);
                }
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}