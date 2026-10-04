package com.example.cv.cv;

import com.example.cv.auth.AuthenticatedUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CvController {

    private final CvService cvService;
    private final CvImportService importService;
    private final CvDuplicationService duplicationService;
    private final AuthenticatedUserService authenticatedUsers;

    public CvController(CvService cvService, CvImportService importService, CvDuplicationService duplicationService,
            AuthenticatedUserService authenticatedUsers) {
        this.cvService = cvService;
        this.importService = importService;
        this.duplicationService = duplicationService;
        this.authenticatedUsers = authenticatedUsers;
    }

    @GetMapping("/cvs")
    public List<CvResponse> findAll(@RequestParam(required = false) UUID personId,
            @AuthenticationPrincipal OidcUser principal) {
        return cvService.findAll(personId, authenticatedUsers.synchronize(principal).getId());
    }

    @GetMapping("/persons/{personId}/cvs")
    public List<CvResponse> findForPerson(@PathVariable UUID personId,
            @AuthenticationPrincipal OidcUser principal) {
        return cvService.findAll(personId, authenticatedUsers.synchronize(principal).getId());
    }

    @GetMapping("/cvs/{id}")
    public CvResponse findById(@PathVariable UUID id) {
        return cvService.findById(id);
    }

    @PostMapping("/persons/{personId}/cvs")
    public ResponseEntity<CvResponse> create(
            @PathVariable UUID personId,
            @Valid @RequestBody CvRequest request) {
        CvResponse cv = cvService.create(personId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/cvs/{id}").buildAndExpand(cv.id()).toUri();
        return ResponseEntity.created(location).body(cv);
    }

    @PostMapping("/persons/{personId}/cvs/import")
    public ResponseEntity<CvResponse> importCv(@PathVariable UUID personId,
            @Valid @RequestBody CvImportRequest request) {
        CvResponse cv = importService.importCv(personId, request);
        return createdCvResponse(cv);
    }

    @PostMapping("/cvs/{id}/duplicate")
    public ResponseEntity<CvResponse> duplicate(@PathVariable UUID id,
            @Valid @RequestBody(required = false) CvDuplicateRequest request,
            @AuthenticationPrincipal OidcUser principal) {
        CvResponse copy = duplicationService.duplicate(id, null, request == null ? null : request.name(),
                request == null ? null : request.language(), request == null ? null : request.personId(),
                authenticatedUsers.synchronize(principal).getId());
        return createdCvResponse(copy);
        }

        @PostMapping("/cvs/{id}/versions/{versionNumber}/branch")
        public ResponseEntity<CvResponse> branch(@PathVariable UUID id, @PathVariable int versionNumber,
            @Valid @RequestBody(required = false) CvDuplicateRequest request,
            @AuthenticationPrincipal OidcUser principal) {
        CvResponse copy = duplicationService.duplicate(id, versionNumber, request == null ? null : request.name(),
                request == null ? null : request.language(), request == null ? null : request.personId(),
                authenticatedUsers.synchronize(principal).getId());
        return createdCvResponse(copy);
        }

        private ResponseEntity<CvResponse> createdCvResponse(CvResponse copy) {
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/cvs/{id}").buildAndExpand(copy.id()).toUri();
        return ResponseEntity.created(location).body(copy);
    }

    @PutMapping("/cvs/{id}")
    public CvResponse update(@PathVariable UUID id, @Valid @RequestBody CvRequest request) {
        return cvService.update(id, request);
    }

    @DeleteMapping("/cvs/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        cvService.delete(id);
        return ResponseEntity.noContent().build();
    }
}