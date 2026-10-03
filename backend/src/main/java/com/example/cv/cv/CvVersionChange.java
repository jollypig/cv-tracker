package com.example.cv.cv;

import com.fasterxml.jackson.databind.JsonNode;

public record CvVersionChange(String path, Type type, JsonNode oldValue, JsonNode newValue) {
    public enum Type {
        ADDED,
        REMOVED,
        MODIFIED
    }
}