package com.example.cv.importing;

import java.util.Objects;

public record NormalizedCvBlock(Type type, String text, Integer pageNumber) {

    public NormalizedCvBlock {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(text, "text");
        if (text.isBlank()) {
            throw new IllegalArgumentException("Block text must not be blank");
        }
        if (pageNumber != null && pageNumber < 1) {
            throw new IllegalArgumentException("Page number must be positive");
        }
    }

    public enum Type {
        TEXT
    }
}