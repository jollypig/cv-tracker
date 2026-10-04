package com.example.cv.importing;

import java.util.Objects;

public record NormalizedCvBlock(Type type, String text, Integer pageNumber, String link) {

    public NormalizedCvBlock {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(text, "text");
        if (text.isBlank()) {
            throw new IllegalArgumentException("Block text must not be blank");
        }
        if (pageNumber != null && pageNumber < 1) {
            throw new IllegalArgumentException("Page number must be positive");
        }
        if (type == Type.LINK && (link == null || link.isBlank())) {
            throw new IllegalArgumentException("Link blocks must include a link target");
        }
    }

    public NormalizedCvBlock(Type type, String text, Integer pageNumber) {
        this(type, text, pageNumber, null);
    }

    public enum Type {
        HEADING,
        PARAGRAPH,
        LIST_ITEM,
        TABLE_ROW,
        LINK,
        TEXT
    }
}