package com.example.cv.cv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.cv.storage.LocalFileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CvExportServiceTest {

    private static final UUID VERSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final byte[] PDF_BYTES = "%PDF-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    @TempDir
    Path directory;

    private CvVersionRepository versions;
    private CvExportRepository exports;
    private CvTemplateRepository templates;
    private CvExportService service;

    @BeforeEach
    void setUp() {
        versions = mock(CvVersionRepository.class);
        exports = mock(CvExportRepository.class);
        templates = mock(CvTemplateRepository.class);
        Cv cv = new Cv(null, "Java Backend", "en", CvStatus.DRAFT);
        setId(cv, UUID.randomUUID());
        CvVersionSnapshot snapshot = new CvVersionSnapshot(null, "Java Backend", null, "en", CvStatus.DRAFT,
                emptyContent(), new CvVersionSnapshot.PersonProfile("Jane", "Doe", "Engineer", "Riga", List.of()));
        CvVersion version = new CvVersion(cv, 3, "Release", new ObjectMapper().valueToTree(snapshot));
        setId(version, VERSION_ID);
        when(versions.findById(VERSION_ID)).thenReturn(Optional.of(version));
        when(versions.existsById(VERSION_ID)).thenReturn(true);
        when(exports.save(any(CvExport.class))).thenAnswer(invocation -> {
            CvExport export = invocation.getArgument(0);
            export.onCreate();
            return export;
        });

        LocalFileStorage storage = new LocalFileStorage(directory.toString());
        PdfRenderer pdfRenderer = html -> {
            assertThat(html).contains("Jane Doe", "Engineer", "Riga");
            return PDF_BYTES;
        };
        service = new CvExportService(versions, exports, templates,
                new CvVersionSnapshotSerializer(new ObjectMapper()), new CvHtmlRenderer(), pdfRenderer, storage);
    }

    @Test
    void exportsSnapshotAndSupportsHistoryAndDownload() throws Exception {
        CvExportResponse response = service.exportPdf(VERSION_ID);

        assertThat(response.versionId()).isEqualTo(VERSION_ID);
        assertThat(response.fileName()).isEqualTo("Jane_Doe_Java_Backend_v3.pdf");
        assertThat(response.fileSize()).isEqualTo(PDF_BYTES.length);
        assertThat(response.downloadUrl()).isEqualTo("/api/v1/exports/" + response.id() + "/download");
        try (var files = Files.list(directory)) {
            assertThat(files.count()).isEqualTo(1);
        }

        org.mockito.ArgumentCaptor<CvExport> exportCaptor = org.mockito.ArgumentCaptor.forClass(CvExport.class);
        verify(exports).save(exportCaptor.capture());
        CvExport saved = exportCaptor.getValue();
        when(exports.findById(response.id())).thenReturn(Optional.of(saved));
        when(exports.findAllByVersion_IdOrderByCreatedAtDesc(VERSION_ID)).thenReturn(List.of(saved));

        assertThat(service.findHistory(VERSION_ID)).containsExactly(response);
        assertThat(service.download(response.id()).content()).containsExactly(PDF_BYTES);
    }

    @Test
    void reportsMissingVersionBeforeCreatingAnExport() {
        UUID missingVersionId = UUID.randomUUID();
        when(versions.findById(missingVersionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exportPdf(missingVersionId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CV version not found");
    }

    private CvContent emptyContent() {
        return new CvContent(null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private void setId(Object entity, UUID id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}