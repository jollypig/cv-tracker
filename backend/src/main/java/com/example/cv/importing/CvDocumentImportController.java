package com.example.cv.importing;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cvs/import")
public class CvDocumentImportController {

    private final CvDocumentImportService importService;

    public CvDocumentImportController(CvDocumentImportService importService) {
        this.importService = importService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CvDocumentImportResponse> startImport(@RequestPart("file") MultipartFile file) {
        CvDocumentImportResponse result = importService.startImport(file);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{importId}").buildAndExpand(result.importId()).toUri();
        return ResponseEntity.created(location).body(result);
    }

    @GetMapping("/{importId}")
    public CvDocumentImportResponse findById(@PathVariable UUID importId) {
        return importService.findById(importId);
    }
}