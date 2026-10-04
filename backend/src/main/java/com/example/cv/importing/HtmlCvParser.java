package com.example.cv.importing;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HtmlCvParser {

    private final long maxFileSizeBytes;

    public HtmlCvParser(@Value("${cv.import.max-file-size-bytes:10485760}") long maxFileSizeBytes) {
        if (maxFileSizeBytes < 1) {
            throw new IllegalArgumentException("Maximum file size must be positive");
        }
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public NormalizedCvDocument parse(byte[] content, String mediaType) {
        if (!isHtmlMediaType(mediaType)) {
            throw new HtmlCvParserException(HtmlCvParserException.Reason.UNSUPPORTED_MEDIA_TYPE,
                    "Only HTML files are supported");
        }
        if (content == null || content.length == 0) {
            throw new HtmlCvParserException(HtmlCvParserException.Reason.INVALID_HTML, "HTML content is empty");
        }
        if (content.length > maxFileSizeBytes) {
            throw new HtmlCvParserException(HtmlCvParserException.Reason.FILE_TOO_LARGE,
                    "HTML exceeds the maximum supported size of " + maxFileSizeBytes + " bytes");
        }

        try {
            Document document = Jsoup.parse(new ByteArrayInputStream(content), null, "");
            List<NormalizedCvBlock> blocks = new ArrayList<>();
            for (Element element : document.body().getAllElements()) {
                String text = element.text();
                switch (element.normalName()) {
                    case "h1", "h2", "h3", "h4", "h5", "h6" -> addBlock(blocks,
                            NormalizedCvBlock.Type.HEADING, text);
                    case "p" -> addBlock(blocks, NormalizedCvBlock.Type.PARAGRAPH, text);
                    case "li" -> addBlock(blocks, NormalizedCvBlock.Type.LIST_ITEM, text);
                    case "tr" -> addBlock(blocks, NormalizedCvBlock.Type.TABLE_ROW, tableRowText(element));
                    case "a" -> addLinkBlock(blocks, element, text);
                    default -> {
                    }
                }
            }
            return new NormalizedCvDocument(blocks);
        } catch (IOException exception) {
            throw new HtmlCvParserException(HtmlCvParserException.Reason.INVALID_HTML,
                    "HTML could not be read", exception);
        }
    }

    private void addBlock(List<NormalizedCvBlock> blocks, NormalizedCvBlock.Type type, String text) {
        if (!text.isBlank()) {
            blocks.add(new NormalizedCvBlock(type, text, null));
        }
    }

    private void addLinkBlock(List<NormalizedCvBlock> blocks, Element element, String text) {
        String link = element.attr("href").strip();
        if (!link.isBlank()) {
            String linkText = text.isBlank() ? link : text;
            blocks.add(new NormalizedCvBlock(NormalizedCvBlock.Type.LINK, linkText, null, link));
        }
    }

    private String tableRowText(Element row) {
        List<Element> cells = row.children().stream()
                .filter(cell -> cell.normalName().equals("th") || cell.normalName().equals("td"))
            .toList();
        if (cells.stream().noneMatch(cell -> !cell.text().isBlank())) {
            return "";
        }
        return cells.stream()
            .map(Element::text)
                .collect(Collectors.joining(" | "));
    }

    private boolean isHtmlMediaType(String mediaType) {
        if (mediaType == null) {
            return false;
        }
        int parameterIndex = mediaType.indexOf(';');
        String baseMediaType = parameterIndex < 0 ? mediaType : mediaType.substring(0, parameterIndex);
        return "text/html".equalsIgnoreCase(baseMediaType.trim())
                || "application/xhtml+xml".equalsIgnoreCase(baseMediaType.trim());
    }
}