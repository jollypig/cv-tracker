package com.example.cv.cv;

import com.example.cv.person.Person;
import com.example.cv.person.PersonRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CvImportServiceTest {

    @Test
    void createsCvForSelectedPersonAndReplacesItsContent() {
        CvRepository cvs = mock(CvRepository.class);
        PersonRepository people = mock(PersonRepository.class);
        CvContentService contentService = mock(CvContentService.class);
        UUID personId = UUID.randomUUID();
        UUID cvId = UUID.randomUUID();
        Person person = new Person("Ada", "Lovelace");
        setId(person, "id", personId);
        when(people.findById(personId)).thenReturn(Optional.of(person));
        when(cvs.save(any(Cv.class))).thenAnswer(invocation -> {
            Cv cv = invocation.getArgument(0);
            setId(cv, "id", cvId);
            return cv;
        });
        CvContent content = new CvContent("Imported summary", List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());
        CvImportRequest request = new CvImportRequest("Imported CV", "Imported notes", "en", CvStatus.ACTIVE,
            List.of("backend"), content);

        CvResponse imported = new CvImportService(new CvService(cvs, people), contentService)
                .importCv(personId, request);

        assertThat(imported.id()).isEqualTo(cvId);
        assertThat(imported.personId()).isEqualTo(personId);
        assertThat(imported.name()).isEqualTo("Imported CV");
        assertThat(imported.status()).isEqualTo(CvStatus.ACTIVE);
        verify(contentService).replace(cvId, content);
    }

    private void setId(Object entity, String fieldName, UUID id) {
        try {
            var field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}