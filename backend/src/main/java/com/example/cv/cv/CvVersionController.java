package com.example.cv.cv;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cvs/{cvId}/versions")
public class CvVersionController {

    private final CvVersionService versionService;

    public CvVersionController(CvVersionService versionService) {
        this.versionService = versionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CvVersionResponse create(@PathVariable UUID cvId,
            @Valid @RequestBody(required = false) CvVersionRequest request) {
        return versionService.create(cvId, request == null ? null : request.description());
    }

    @GetMapping
    public List<CvVersionResponse> findAll(@PathVariable UUID cvId) {
        return versionService.findAll(cvId);
    }

    @GetMapping("/diff")
    public CvVersionDiffResponse diff(@PathVariable UUID cvId,
            @RequestParam int fromVersion, @RequestParam int toVersion) {
        return versionService.diff(cvId, fromVersion, toVersion);
    }

    @GetMapping("/{versionNumber}")
    public CvVersionDetailResponse find(@PathVariable UUID cvId, @PathVariable int versionNumber) {
        return versionService.find(cvId, versionNumber);
    }

    @PostMapping("/{versionNumber}/restore")
    @ResponseStatus(HttpStatus.CREATED)
    public CvVersionResponse restore(@PathVariable UUID cvId, @PathVariable int versionNumber,
            @Valid @RequestBody(required = false) CvVersionRequest request) {
        return versionService.restore(cvId, versionNumber, request == null ? null : request.description());
    }
}