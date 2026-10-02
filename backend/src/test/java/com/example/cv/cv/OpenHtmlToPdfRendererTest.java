package com.example.cv.cv;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class OpenHtmlToPdfRendererTest {

    private final OpenHtmlToPdfRenderer renderer = new OpenHtmlToPdfRenderer();

    @Test
    void rendersHtmlToPdfBytes() {
        byte[] pdf = renderer.render("<html><body><h1>CV</h1></body></html>");

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(pdf).hasSizeGreaterThan(100);
    }

    @Test
    void producesReadableMultiPageCvLayout() throws Exception {
        String summary = "A careful engineer with production experience. ".repeat(180);
        CvContent content = new CvContent(summary, java.util.List.of(), java.util.List.of(), java.util.List.of(),
                java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(),
                java.util.List.of(new CvContent.Section(CvSectionType.SUMMARY, true, 0)));
        CvVersionSnapshot snapshot = new CvVersionSnapshot(null, "Resume", null, "en", CvStatus.DRAFT, content,
                new CvVersionSnapshot.PersonProfile("Jane", "Doe", "Engineer", "Riga", java.util.List.of()));

        byte[] pdf = renderer.render(new CvHtmlRenderer().render(snapshot, "modern"));

        try (PDDocument document = PDDocument.load(pdf)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            assertThat(new PDFTextStripper().getText(document))
                    .contains("Jane Doe", "Engineer", "A careful engineer");
        }
    }
}