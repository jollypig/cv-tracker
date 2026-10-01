package com.example.cv.person;

import java.util.UUID;

public record PersonContactResponse(
        UUID id,
        ContactType type,
        String value,
        boolean primary,
        int sortOrder) {
}