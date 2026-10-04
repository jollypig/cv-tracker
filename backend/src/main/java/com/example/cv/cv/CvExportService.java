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
    private final DocxRenderer docxRenderer;
    private final FileStorage storage;

    public CvExportService(CvVersionRepository versionRepository, CvExportRepository exportRepository,
            CvTemplateRepository templateRepository, CvVersionSnapshotSerializer snapshotSerializer,
            CvHtmlRenderer htmlRenderer, PdfRenderer pdfRenderer, DocxRenderer docxRenderer, FileStorage storage) {
        this.versionRepository = versionRepository;
        this.exportRepository = exportRepository;
        this.templateRepository = templateRepository;
        this.snapshotSerializer = snapshotSerializer;
        this.htmlRenderer = htmlRenderer;
        this.pdfRenderer = pdfRenderer;
        this.docxRenderer = docxRenderer;
        this.storage = storage;
    }

    public CvExportResponse exportPdf(UUID versionId) {
        CvVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV version not found"));
        CvVersionSnapshot snapshot = snapshotSerializer.deserialize(version.getSnapshot());
        String templateKey = snapshot.templateId() == null ? "modern"
                : templateRepository.findById(snapshot.templateId()).map(CvTemplate::getTemplateKey).orElse("modern");
        byte[] pdf = pdfRenderer.render(htmlRenderer.render(snapshot, templateKey));
        return saveExport(version, snapshot, pdf, "pdf", "application/pdf");
        }

        public CvExportResponse exportDocx(UUID versionId) {
        CvVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV version not found"));
        CvVersionSnapshot snapshot = snapshotSerializer.deserialize(version.getSnapshot());
        byte[] docx = docxRenderer.render(snapshot);
        return saveExport(version, snapshot, docx, "docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }

        private CvExportResponse saveExport(CvVersion version, CvVersionSnapshot snapshot, byte[] content,
            String extension, String contentType) {
        CvVersionSnapshot.PersonProfile person = snapshot.person();
        String firstName = person == null ? null : person.firstName();
        String lastName = person == null ? null : person.lastName();
        String fileName = CvExportFileName.generate(firstName, lastName, snapshot.name(), version.getVersionNumber(),
            extension);
        UUID exportId = UUID.randomUUID();
        String storageKey = exportId + "." + extension;
        storage.upload(storageKey, new ByteArrayInputStream(content), contentType);
        try {
            CvExport saved = exportRepository.save(new CvExport(exportId, version, fileName, storageKey, content.length));
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
            return new CvExportFile(export.getFileName(), content.readAllBytes(), contentType(export.getFileName()));
        } catch (IllegalStateException exception) {
            if (exception.getCause() instanceof IOException) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CV export file not found");
            }
            throw exception;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read CV export", exception);
        }
    }

    private String contentType(String fileName) {
        return fileName.endsWith(".docx")
                ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                : "application/pdf";
    }
}