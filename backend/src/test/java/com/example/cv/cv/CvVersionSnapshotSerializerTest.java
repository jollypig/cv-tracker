package com.example.cv.cv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.cv.person.Person;
import com.example.cv.person.PersonContact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CvVersionSnapshotSerializerTest {

    @Test
    void capturesProfileAndContactsInVersionSnapshot() {
        Person person = new Person("Jane", "Doe");
        person.setPosition("Engineer");
        person.setLocation("Riga");
        person.setContacts(List.of(new PersonContact("EMAIL", "jane@example.test", true, 0)));
        Cv cv = new Cv(person, "Resume", "en", CvStatus.DRAFT);
        CvContent content = new CvContent(null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of());
        CvVersionSnapshotSerializer serializer = new CvVersionSnapshotSerializer(new ObjectMapper());

        CvVersionSnapshot snapshot = serializer.deserialize(serializer.serialize(cv, content));

        assertThat(snapshot.person().firstName()).isEqualTo("Jane");
        assertThat(snapshot.person().position()).isEqualTo("Engineer");
        assertThat(snapshot.person().contacts()).containsExactly(
                new CvVersionSnapshot.Contact("EMAIL", "jane@example.test", 0));
    }
}