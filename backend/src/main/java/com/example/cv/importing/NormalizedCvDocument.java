package com.example.cv.importing;

import java.util.List;
import java.util.Objects;

public record NormalizedCvDocument(List<NormalizedCvBlock> blocks) {

    public NormalizedCvDocument {
        Objects.requireNonNull(blocks, "blocks");
        blocks = List.copyOf(blocks);
    }
}