package com.example.cv.importing;

import com.example.cv.auth.AuthenticatedUserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final AuthenticatedUserService authenticatedUsers;

    public CvDocumentImportController(CvDocumentImportService importService,
            AuthenticatedUserService authenticatedUsers) {
        this.importService = importService;
        this.authenticatedUsers = authenticatedUsers;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CvDocumentImportResponse> startImport(@RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal OidcUser principal) {
        CvDocumentImportResponse result = importService.startImport(file, ownerId(principal));
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{importId}").buildAndExpand(result.importId()).toUri();
        return ResponseEntity.created(location).body(result);
    }

    @GetMapping("/{importId}")
    public CvDocumentImportResponse findById(@PathVariable UUID importId,
            @AuthenticationPrincipal OidcUser principal) {
        return importService.findById(importId, ownerId(principal));
    }

    @PutMapping("/{importId}/draft")
    public CvDocumentImportResponse updateDraft(@PathVariable UUID importId,
            @Valid @RequestBody ParsedCv draft, @AuthenticationPrincipal OidcUser principal) {
        return importService.updateDraft(importId, ownerId(principal), draft);
    }

    @PostMapping("/{importId}/approve")
    public CvDocumentImportResponse approveDraft(@PathVariable UUID importId,
            @Valid @RequestBody CvDraftApprovalRequest request, @AuthenticationPrincipal OidcUser principal) {
        return importService.approveDraft(importId, ownerId(principal), request);
    }

    private UUID ownerId(OidcUser principal) {
        return authenticatedUsers.synchronize(principal).getId();
    }
}