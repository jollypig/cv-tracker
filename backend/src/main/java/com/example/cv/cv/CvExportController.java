package com.example.cv.cv;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CvExportController {

    private final CvExportService exportService;

    public CvExportController(CvExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping("/cv-versions/{versionId}/exports/pdf")
    @ResponseStatus(HttpStatus.CREATED)
    public CvExportResponse exportPdf(@PathVariable UUID versionId) {
        return exportService.exportPdf(versionId);
    }

    @GetMapping("/cv-versions/{versionId}/exports")
    public List<CvExportResponse> findHistory(@PathVariable UUID versionId) {
        return exportService.findHistory(versionId);
    }

    @GetMapping("/exports/{exportId}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID exportId) {
        CvExportFile file = exportService.download(exportId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(file.content());
    }
}