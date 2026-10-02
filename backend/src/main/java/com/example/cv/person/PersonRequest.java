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
        @Size(max = 255) String position,
        @Size(max = 20) String gender,
        @Size(max = 20) String maritalStatus,
        @Size(max = 30) String militaryStatus,
        @Size(max = 255) String location,
        @Size(max = 500) String photoStorageKey,
        List<@Valid PersonContactRequest> contacts) {
}