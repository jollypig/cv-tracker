package com.example.cv.importing;

import java.util.ArrayList;
import java.util.List;

final class CvDocumentNormalizer {

    private CvDocumentNormalizer() {
    }

    static List<NormalizedCvBlock> normalize(List<NormalizedCvBlock> blocks) {
        List<NormalizedCvBlock> normalizedBlocks = new ArrayList<>();
        for (NormalizedCvBlock block : blocks) {
            String text = normalizeText(block.text());
            if (!text.isBlank()) {
                normalizedBlocks.add(new NormalizedCvBlock(block.type(), text, block.pageNumber(), block.link()));
            }
        }
        return List.copyOf(normalizedBlocks);
    }

    private static String normalizeText(String text) {
        StringBuilder sanitized = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '\r') {
                if (index + 1 < text.length() && text.charAt(index + 1) == '\n') {
                    index++;
                }
                sanitized.append('\n');
            } else if (character == '\f') {
                sanitized.append('\n');
            } else if (character == '\u200B' || character == '\uFEFF' || character == '\u00AD') {
                continue;
            } else if (!Character.isISOControl(character) || character == '\n' || character == '\t') {
                sanitized.append(character == '\u00A0' || character == '\u202F' ? ' ' : character);
            }
        }

        List<String> lines = new ArrayList<>();
        for (String line : sanitized.toString().split("\n", -1)) {
            String normalizedLine = line.replaceAll("[\\t ]+", " ").strip();
            if (normalizedLine.isEmpty()) {
                if (!lines.isEmpty() && !lines.get(lines.size() - 1).isEmpty()) {
                    lines.add("");
                }
            } else {
                lines.add(normalizedLine);
            }
        }
        return String.join("\n", lines).strip();
    }
}