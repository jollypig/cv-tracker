package com.example.cv.person;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PersonRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @PastOrPresent LocalDate dateOfBirth,
        @Size(max = 255) String headline,
        @Size(max = 500) String photoStorageKey,
        List<@Valid PersonContactRequest> contacts) {
}