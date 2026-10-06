package com.example.cv.person;

import java.util.UUID;

public record PersonContactResponse(
        UUID id,
        ContactType type,
        String value,
        boolean primary,
                int sortOrder,
                boolean showContact) {

        public PersonContactResponse(UUID id, ContactType type, String value, boolean primary, int sortOrder) {
                this(id, type, value, primary, sortOrder, true);
        }
}