package com.example.cv.importing;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfCvParserTest {

    private final PdfCvParser parser = new PdfCvParser(10 * 1024 * 1024);

    @Test
    void extractsTextAndRetainsPageBoundaries() throws IOException {
        NormalizedCvDocument document = parser.parse(createPdf("Ada Lovelace", "Analytical Engine"), "application/pdf");

        assertThat(document.blocks())
                .extracting(NormalizedCvBlock::text)
                .containsExactly("Ada Lovelace", "Analytical Engine");
        assertThat(document.blocks())
                .extracting(NormalizedCvBlock::pageNumber)
                .containsExactly(1, 2);
    }

    @Test
    void reportsTextlessPdfAsRequiringOcr() throws IOException {
        assertThatThrownBy(() -> parser.parse(createImageOnlyPdf(), "application/pdf"))
                .isInstanceOf(PdfCvParserException.class)
                .extracting(exception -> ((PdfCvParserException) exception).reason())
                .isEqualTo(PdfCvParserException.Reason.OCR_REQUIRED);
    }

    @Test
    void rejectsUnsupportedMediaType() {
        assertThatThrownBy(() -> parser.parse(new byte[]{1}, "text/plain"))
                .isInstanceOf(PdfCvParserException.class)
                .extracting(exception -> ((PdfCvParserException) exception).reason())
                .isEqualTo(PdfCvParserException.Reason.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void rejectsOversizedPdfBeforeParsing() {
        assertThatThrownBy(() -> new PdfCvParser(1).parse(new byte[]{1, 2}, "application/pdf"))
                .isInstanceOf(PdfCvParserException.class)
                .extracting(exception -> ((PdfCvParserException) exception).reason())
                .isEqualTo(PdfCvParserException.Reason.FILE_TOO_LARGE);
    }

    @Test
    void reportsMalformedPdfAsInvalid() {
        assertThatThrownBy(() -> parser.parse(new byte[]{1, 2, 3}, "application/pdf"))
                .isInstanceOf(PdfCvParserException.class)
                .extracting(exception -> ((PdfCvParserException) exception).reason())
                .isEqualTo(PdfCvParserException.Reason.INVALID_PDF);
    }

    private byte[] createPdf(String... pageTexts) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String pageText : pageTexts) {
                PDPage page = new PDPage();
                document.addPage(page);
                if (!pageText.isEmpty()) {
                    try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                        content.beginText();
                        content.setFont(PDType1Font.HELVETICA, 12);
                        content.newLineAtOffset(72, 720);
                        content.showText(pageText);
                        content.endText();
                    }
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] createImageOnlyPdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
            image.setRGB(0, 0, Color.BLACK.getRGB());
            var imageObject = LosslessFactory.createFromImage(document, image);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.drawImage(imageObject, 0, 0);
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}