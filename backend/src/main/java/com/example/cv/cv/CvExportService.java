package com.example.cv.cv;

import com.example.cv.storage.FileStorage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CvExportService {

    private final CvVersionRepository versionRepository;
    private final CvExportRepository exportRepository;
    private final CvTemplateRepository templateRepository;
    private final CvVersionSnapshotSerializer snapshotSerializer;
    private final CvHtmlRenderer htmlRenderer;
    private final PdfRenderer pdfRenderer;
    private final FileStorage storage;

    public CvExportService(CvVersionRepository versionRepository, CvExportRepository exportRepository,
            CvTemplateRepository templateRepository, CvVersionSnapshotSerializer snapshotSerializer,
            CvHtmlRenderer htmlRenderer, PdfRenderer pdfRenderer, FileStorage storage) {
        this.versionRepository = versionRepository;
        this.exportRepository = exportRepository;
        this.templateRepository = templateRepository;
        this.snapshotSerializer = snapshotSerializer;
        this.htmlRenderer = htmlRenderer;
        this.pdfRenderer = pdfRenderer;
        this.storage = storage;
    }

    public CvExportResponse exportPdf(UUID versionId) {
        CvVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV version not found"));
        CvVersionSnapshot snapshot = snapshotSerializer.deserialize(version.getSnapshot());
        String templateKey = snapshot.templateId() == null ? "modern"
                : templateRepository.findById(snapshot.templateId()).map(CvTemplate::getTemplateKey).orElse("modern");
        byte[] pdf = pdfRenderer.render(htmlRenderer.render(snapshot, templateKey));
        CvVersionSnapshot.PersonProfile person = snapshot.person();
        String firstName = person == null ? null : person.firstName();
        String lastName = person == null ? null : person.lastName();
        String fileName = CvExportFileName.generate(firstName, lastName, snapshot.name(), version.getVersionNumber());
        UUID exportId = UUID.randomUUID();
        String storageKey = exportId + ".pdf";
        storage.upload(storageKey, new ByteArrayInputStream(pdf), "application/pdf");
        try {
            CvExport saved = exportRepository.save(new CvExport(exportId, version, fileName, storageKey, pdf.length));
            return CvExportResponse.from(saved);
        } catch (RuntimeException exception) {
            storage.delete(storageKey);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<CvExportResponse> findHistory(UUID versionId) {
        if (!versionRepository.existsById(versionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CV version not found");
        }
        return exportRepository.findAllByVersion_IdOrderByCreatedAtDesc(versionId).stream()
                .map(CvExportResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CvExportFile download(UUID exportId) {
        CvExport export = exportRepository.findById(exportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV export not found"));
        try (InputStream content = storage.download(export.getStorageKey())) {
            return new CvExportFile(export.getFileName(), content.readAllBytes());
        } catch (IllegalStateException exception) {
            if (exception.getCause() instanceof IOException) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CV export file not found");
            }
            throw exception;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read CV PDF", exception);
        }
    }
}