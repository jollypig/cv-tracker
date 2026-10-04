package com.example.cv.importing;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfCvParser {

    private final long maxFileSizeBytes;

    public PdfCvParser(@Value("${cv.import.max-file-size-bytes:10485760}") long maxFileSizeBytes) {
        if (maxFileSizeBytes < 1) {
            throw new IllegalArgumentException("Maximum file size must be positive");
        }
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public NormalizedCvDocument parse(byte[] content, String mediaType) {
        if (!isPdfMediaType(mediaType)) {
            throw new PdfCvParserException(PdfCvParserException.Reason.UNSUPPORTED_MEDIA_TYPE,
                    "Only application/pdf files are supported");
        }
        if (content == null || content.length == 0) {
            throw new PdfCvParserException(PdfCvParserException.Reason.INVALID_PDF, "PDF content is empty");
        }
        if (content.length > maxFileSizeBytes) {
            throw new PdfCvParserException(PdfCvParserException.Reason.FILE_TOO_LARGE,
                    "PDF exceeds the maximum supported size of " + maxFileSizeBytes + " bytes");
        }

        try (PDDocument document = PDDocument.load(content)) {
            if (document.getNumberOfPages() == 0) {
                throw new PdfCvParserException(PdfCvParserException.Reason.INVALID_PDF,
                        "PDF does not contain any pages");
            }

            List<NormalizedCvBlock> blocks = new ArrayList<>();
            for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                String text = normalizeLineEndings(stripper.getText(document)).strip();
                if (!text.isBlank()) {
                    blocks.add(new NormalizedCvBlock(NormalizedCvBlock.Type.TEXT, text, pageNumber));
                }
            }

            if (blocks.stream().noneMatch(block -> block.text().codePoints()
                    .anyMatch(Character::isLetterOrDigit))) {
                throw new PdfCvParserException(PdfCvParserException.Reason.OCR_REQUIRED,
                        "PDF contains no extractable text; OCR is required");
            }
            return new NormalizedCvDocument(blocks);
        } catch (IOException exception) {
            throw new PdfCvParserException(PdfCvParserException.Reason.INVALID_PDF,
                    "PDF could not be read", exception);
        }
    }

    private boolean isPdfMediaType(String mediaType) {
        if (mediaType == null) {
            return false;
        }
        int parameterIndex = mediaType.indexOf(';');
        String baseMediaType = parameterIndex < 0 ? mediaType : mediaType.substring(0, parameterIndex);
        return "application/pdf".equalsIgnoreCase(baseMediaType.trim());
    }

    private String normalizeLineEndings(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }
}
