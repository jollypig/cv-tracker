package com.example.cv.importing;

import java.util.List;
import java.util.Objects;

public record PersonalData(
        ExtractedValue<String> firstName,
        ExtractedValue<String> lastName,
        ExtractedValue<String> email,
        ExtractedValue<String> phone,
        ExtractedValue<String> location,
        List<ExtractedValue<String>> urls
) {

    public PersonalData {
        Objects.requireNonNull(urls, "urls");
        urls = List.copyOf(urls);
    }
}