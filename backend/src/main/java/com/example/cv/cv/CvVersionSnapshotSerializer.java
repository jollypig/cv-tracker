package com.example.cv.cv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.cv.person.Person;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class CvVersionSnapshotSerializer {

    private final ObjectMapper objectMapper;

    public CvVersionSnapshotSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode serialize(Cv cv, CvContent content) {
        Person person = cv.getPerson();
        CvVersionSnapshot.PersonProfile profile = person == null ? null : new CvVersionSnapshot.PersonProfile(
            person.getFirstName(), person.getLastName(), person.getPosition(), person.getLocation(),
            person.getContacts().stream()
                .sorted(Comparator.comparingInt(contact -> contact.getSortOrder()))
                .map(contact -> new CvVersionSnapshot.Contact(
                    contact.getType(), contact.getValue(), contact.getSortOrder(), contact.isShowContact()))
                .toList(), person.getPhotoStorageKey(), true);
        return objectMapper.valueToTree(new CvVersionSnapshot(cv.getTemplateId(), cv.getName(),
            cv.getDescription(), cv.getLanguage(), cv.getStatus(), content, profile,
            cv.getTags().stream().sorted().toList()));
    }

    public CvVersionSnapshot deserialize(JsonNode snapshot) {
        return objectMapper.convertValue(snapshot, CvVersionSnapshot.class);
    }

}