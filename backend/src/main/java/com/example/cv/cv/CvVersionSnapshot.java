package com.example.cv.cv;

import java.util.UUID;
import java.util.List;

public record CvVersionSnapshot(
        UUID templateId,
        String name,
        String description,
        String language,
        CvStatus status,
        CvContent content,
                PersonProfile person,
                List<String> tags) {

        public CvVersionSnapshot(UUID templateId, String name, String description, String language,
                        CvStatus status, CvContent content, PersonProfile person) {
                this(templateId, name, description, language, status, content, person, List.of());
        }

    public record PersonProfile(
            String firstName,
            String lastName,
            String position,
            String location,
                        java.util.List<Contact> contacts,
                        String photoStorageKey,
                        Boolean showContacts) {

                public PersonProfile(String firstName, String lastName, String position, String location,
                                List<Contact> contacts) {
                        this(firstName, lastName, position, location, contacts, null, true);
                }

                public PersonProfile(String firstName, String lastName, String position, String location,
                                List<Contact> contacts, String photoStorageKey) {
                        this(firstName, lastName, position, location, contacts, photoStorageKey, true);
                }
    }

        public record Contact(String type, String value, int sortOrder, Boolean showContact) {
                public Contact(String type, String value, int sortOrder) {
                        this(type, value, sortOrder, true);
                }
    }

}