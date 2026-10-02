package com.example.cv.cv;

import java.util.regex.Pattern;

public final class CvExportFileName {

    private static final Pattern INVALID_FILENAME_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private CvExportFileName() {
    }

    public static String generate(String firstName, String lastName, String cvName, int versionNumber) {
        return component(firstName) + "_" + component(lastName) + "_" + component(cvName)
                + "_v" + versionNumber + ".pdf";
    }

    private static String component(String value) {
        String normalized = value == null ? "" : value.trim();
        normalized = INVALID_FILENAME_CHARACTERS.matcher(normalized).replaceAll("_");
        normalized = WHITESPACE.matcher(normalized).replaceAll("_");
        normalized = normalized.replaceAll("_+", "_").replaceAll("^_|_$", "");
        return normalized.isBlank() ? "cv" : normalized;
    }
}