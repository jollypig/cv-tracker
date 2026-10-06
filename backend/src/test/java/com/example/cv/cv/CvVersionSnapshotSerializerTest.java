package com.example.cv.cv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.cv.person.Person;
import com.example.cv.person.PersonContact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CvVersionSnapshotSerializerTest {

    @Test
    void preservesSkillDetailsAndProjectLinksInSnapshots() {
    var details = new CvSkillDetails(new java.math.BigDecimal("4.5"), null, "2024", "2010", "daily",
        "active", List.of(new CvSkillDetails.ProjectLink("stable", "Delivered")), true);
    var content = new CvContent(null, List.of(), List.of(), List.of(new CvContent.SkillGroup("Backend", 0,
        List.of(new CvContent.Skill("Java", null, 0, true, details)))), List.of(),
        List.of(new CvContent.Project("Platform", null, null, null, null, 0, "stable",
            java.time.LocalDate.of(2020, 1, 1), java.time.LocalDate.of(2024, 1, 1), false)),
        List.of(), List.of(), List.of());
    var serializer = new CvVersionSnapshotSerializer(new ObjectMapper().findAndRegisterModules());
    var snapshot = serializer.deserialize(serializer.serialize(new Cv(new Person("Jane", "Doe"),
        "Resume", "en", CvStatus.DRAFT), content));
    assertThat(snapshot.content()).isEqualTo(content);
    }

    @Test
    void capturesProfileAndContactsInVersionSnapshot() {
        Person person = new Person("Jane", "Doe");
        person.setPosition("Engineer");
        person.setLocation("Riga");
        person.setPhotoStorageKey("persons/person-id/photo.png");
        person.setContacts(List.of(new PersonContact("EMAIL", "jane@example.test", true, 0)));
        Cv cv = new Cv(person, "Resume", "en", CvStatus.DRAFT);
        CvContent content = new CvContent(null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of());
        CvVersionSnapshotSerializer serializer = new CvVersionSnapshotSerializer(new ObjectMapper());

        CvVersionSnapshot snapshot = serializer.deserialize(serializer.serialize(cv, content));

        assertThat(snapshot.person().firstName()).isEqualTo("Jane");
        assertThat(snapshot.person().position()).isEqualTo("Engineer");
        assertThat(snapshot.person().photoStorageKey()).isEqualTo("persons/person-id/photo.png");
        assertThat(snapshot.person().contacts()).containsExactly(
                new CvVersionSnapshot.Contact("EMAIL", "jane@example.test", 0));
    }
}