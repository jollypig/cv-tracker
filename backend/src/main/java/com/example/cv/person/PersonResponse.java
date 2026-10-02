package com.example.cv.person;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PersonResponse(
        UUID id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String position,
        String gender,
        String maritalStatus,
        String militaryStatus,
        String location,
        String photoStorageKey,
        List<PersonContactResponse> contacts,
        Instant createdAt,
        Instant updatedAt) {
}