package com.example.cv.cv;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CvVersionServiceTest {

    private static final UUID CV_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void createsSnapshotsWithSequentialNumbersAndOptionalDescription() {
        CvRepository cvs = mock(CvRepository.class);
        CvVersionRepository versions = mock(CvVersionRepository.class);
        CvContentService content = mock(CvContentService.class);
        Cv cv = new Cv(null, "Resume", "en", CvStatus.DRAFT);
        setCvId(cv);
        CvContent current = emptyContent("current");
        when(cvs.findByIdForUpdate(CV_ID)).thenReturn(Optional.of(cv));
        when(content.get(CV_ID)).thenReturn(current);
        when(versions.findTopByCv_IdOrderByVersionNumberDesc(CV_ID)).thenReturn(Optional.empty());
        when(versions.save(any(CvVersion.class))).thenAnswer(invocation -> {
            CvVersion version = invocation.getArgument(0);
            setVersionId(version);
            return version;
        });

        CvVersionResponse created = service(cvs, versions, content).create(CV_ID, " first snapshot ");

        assertThat(created.versionNumber()).isEqualTo(1);
        assertThat(created.description()).isEqualTo("first snapshot");
        assertThat(cv.getCurrentVersionId()).isNotNull();
        verify(cvs).save(cv);
    }

    @Test
    void restoresContentAsNextVersionWithoutChangingTheSourceSnapshot() {
        CvRepository cvs = mock(CvRepository.class);
        CvVersionRepository versions = mock(CvVersionRepository.class);
        CvContentService content = mock(CvContentService.class);
        Cv cv = new Cv(null, "Current", "en", CvStatus.DRAFT);
        setCvId(cv);
        CvVersionSnapshotSerializer serializer = serializer();
        CvVersion source = new CvVersion(cv, 1, "original", serializer.serialize(cv, emptyContent("saved")));
        when(cvs.findByIdForUpdate(CV_ID)).thenReturn(Optional.of(cv));
        when(versions.findByCv_IdAndVersionNumber(CV_ID, 1)).thenReturn(Optional.of(source));
        when(versions.findTopByCv_IdOrderByVersionNumberDesc(CV_ID)).thenReturn(Optional.of(source));
        when(versions.save(any(CvVersion.class))).thenAnswer(invocation -> {
            CvVersion version = invocation.getArgument(0);
            setVersionId(version);
            return version;
        });

        CvVersionResponse restored = service(cvs, versions, content, serializer).restore(CV_ID, 1, null);

        assertThat(restored.versionNumber()).isEqualTo(2);
        assertThat(restored.description()).isEqualTo("Restored from version 1");
        verify(content).replace(CV_ID, emptyContent("saved"));
        assertThat(serializer.deserialize(source.getSnapshot()).content().summary()).isEqualTo("saved");
    }

    @Test
    void createsVersionWithExplicitParentLineage() {
        CvRepository cvs = mock(CvRepository.class);
        CvVersionRepository versions = mock(CvVersionRepository.class);
        CvContentService content = mock(CvContentService.class);
        Cv cv = new Cv(null, "Branch", "en", CvStatus.DRAFT);
        setCvId(cv);
        CvVersion parent = mock(CvVersion.class);
        UUID parentId = UUID.randomUUID();
        when(parent.getId()).thenReturn(parentId);
        when(parent.getCv()).thenReturn(cv);
        when(parent.getVersionNumber()).thenReturn(4);
        when(cvs.findByIdForUpdate(CV_ID)).thenReturn(Optional.of(cv));
        when(content.get(CV_ID)).thenReturn(emptyContent("branch"));
        when(versions.findById(parentId)).thenReturn(Optional.of(parent));
        when(versions.findTopByCv_IdOrderByVersionNumberDesc(CV_ID)).thenReturn(Optional.empty());
        when(versions.save(any(CvVersion.class))).thenAnswer(invocation -> {
            CvVersion saved = invocation.getArgument(0);
            setVersionId(saved);
            return saved;
        });

        CvVersionResponse created = service(cvs, versions, content).create(CV_ID, "Branch", parentId);

        assertThat(created.parentVersionId()).isEqualTo(parentId);
        assertThat(created.parentCvId()).isEqualTo(CV_ID);
        assertThat(created.parentVersionNumber()).isEqualTo(4);
    }

    @Test
    void reportsMissingVersions() {
        CvRepository cvs = mock(CvRepository.class);
        CvVersionRepository versions = mock(CvVersionRepository.class);
        when(versions.findByCv_IdAndVersionNumber(CV_ID, 3)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service(cvs, versions, mock(CvContentService.class)).find(CV_ID, 3))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CV version not found");
    }

    private CvVersionService service(CvRepository cvs, CvVersionRepository versions, CvContentService content) {
        return service(cvs, versions, content, serializer());
    }

    private CvVersionService service(CvRepository cvs, CvVersionRepository versions, CvContentService content,
            CvVersionSnapshotSerializer serializer) {
        return new CvVersionService(cvs, versions, content, serializer);
    }

    private CvVersionSnapshotSerializer serializer() {
        return new CvVersionSnapshotSerializer(new ObjectMapper().findAndRegisterModules());
    }

    private CvContent emptyContent(String summary) {
        return new CvContent(summary, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private void setVersionId(CvVersion version) {
        try {
            var field = CvVersion.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(version, UUID.randomUUID());
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private void setCvId(Cv cv) {
        try {
            var field = Cv.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(cv, CV_ID);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}