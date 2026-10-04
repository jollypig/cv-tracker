package com.example.cv.cv;

import com.example.cv.person.Person;
import com.example.cv.person.PersonRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CvDuplicationServiceTest {

    @Test
    void branchesFromSelectedVersionSnapshotAndLinksTheInitialVersion() {
        CvRepository cvs = mock(CvRepository.class);
        PersonRepository people = mock(PersonRepository.class);
        CvContentService content = mock(CvContentService.class);
        CvVersionRepository versions = mock(CvVersionRepository.class);
        CvVersionService versionService = mock(CvVersionService.class);
        CvVersionSnapshotSerializer serializer = new CvVersionSnapshotSerializer(new ObjectMapper().findAndRegisterModules());
        UUID sourceId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID targetPersonId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Person person = new Person("Ada", "Lovelace");
        Person targetPerson = new Person("Grace", "Hopper");
        setId(targetPerson, "id", targetPersonId);
        Cv source = new Cv(person, "Original", "en", CvStatus.ACTIVE);
        setId(source, "id", sourceId);
        Cv sourceVersionCv = new Cv(person, "Original", "en", CvStatus.ACTIVE);
        setId(sourceVersionCv, "id", sourceId);
        CvVersionSnapshot historical = new CvVersionSnapshot(null, "Historical title", "Historical description", "fr",
                CvStatus.ACTIVE, content("historical content"), null);
        CvVersion parent = new CvVersion(sourceVersionCv, 2, "Tailored", new ObjectMapper().valueToTree(historical));
        setId(parent, "id", parentId);
        when(cvs.findById(sourceId)).thenReturn(Optional.of(source));
        when(people.findByIdAndOwner_Id(targetPersonId, ownerId)).thenReturn(Optional.of(targetPerson));
        when(versions.findByCv_IdAndVersionNumber(sourceId, 2)).thenReturn(Optional.of(parent));
        when(cvs.save(any(Cv.class))).thenAnswer(invocation -> {
            Cv saved = invocation.getArgument(0);
            setId(saved, "id", branchId);
            return saved;
        });
        CvVersionResponse initial = new CvVersionResponse(versionId, branchId, 1, "Copied from Historical title",
                Instant.parse("2026-01-01T00:00:00Z"), parentId, sourceId, 2);
        when(versionService.create(branchId, "Copied from Historical title", parentId)).thenReturn(initial);

        CvResponse branch = new CvDuplicationService(cvs, people, content, versions, versionService, serializer)
            .duplicate(sourceId, 2, "Branch name", "lv", targetPersonId, ownerId);

        assertThat(branch.name()).isEqualTo("Branch name");
        assertThat(branch.personId()).isEqualTo(targetPersonId);
        assertThat(branch.personName()).isEqualTo("Grace Hopper");
        assertThat(branch.language()).isEqualTo("lv");
        assertThat(branch.status()).isEqualTo(CvStatus.DRAFT);
        verify(content).replace(branchId, content("historical content"));
        verify(versionService).create(branchId, "Copied from Historical title", parentId);
    }

    @Test
    void rejectsDestinationPersonNotOwnedByCurrentUser() {
        CvRepository cvs = mock(CvRepository.class);
        PersonRepository people = mock(PersonRepository.class);
        UUID sourceId = UUID.randomUUID();
        UUID targetPersonId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Cv source = new Cv(new Person("Ada", "Lovelace"), "Original", "en", CvStatus.ACTIVE);
        setId(source, "id", sourceId);
        when(cvs.findById(sourceId)).thenReturn(Optional.of(source));

        CvDuplicationService service = new CvDuplicationService(cvs, people, mock(CvContentService.class),
                mock(CvVersionRepository.class), mock(CvVersionService.class),
                new CvVersionSnapshotSerializer(new ObjectMapper().findAndRegisterModules()));

        assertThatThrownBy(() -> service.duplicate(sourceId, null, "Copy", null, targetPersonId, ownerId))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.NOT_FOUND);
        verify(cvs, org.mockito.Mockito.never()).save(any(Cv.class));
    }

    private CvContent content(String summary) {
        return new CvContent(summary, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
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