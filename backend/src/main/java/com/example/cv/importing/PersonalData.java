package com.example.cv.importing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

public record PersonalData(
    @Valid ExtractedValue<String> firstName,
    @Valid ExtractedValue<String> lastName,
    @Valid ExtractedValue<String> email,
    @Valid ExtractedValue<String> phone,
    @Valid ExtractedValue<String> location,
    @NotNull List<@NotNull @Valid ExtractedValue<String>> urls
) {

    public PersonalData {
        Objects.requireNonNull(urls, "urls");
        urls = List.copyOf(urls);
    }
}