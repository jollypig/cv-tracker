package com.example.cv.cv;

import java.util.UUID;

public record CvVersionSnapshot(
        UUID templateId,
        String name,
        String description,
        String language,
        CvStatus status,
        CvContent content,
        PersonProfile person) {

    public record PersonProfile(
            String firstName,
            String lastName,
            String position,
            String location,
            java.util.List<Contact> contacts) {
    }

    public record Contact(String type, String value, int sortOrder) {
    }
}