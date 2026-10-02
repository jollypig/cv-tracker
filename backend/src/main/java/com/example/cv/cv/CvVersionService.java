package com.example.cv.cv;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CvVersionService {

    private final CvRepository cvRepository;
    private final CvVersionRepository versionRepository;
    private final CvContentService contentService;
    private final CvVersionSnapshotSerializer snapshotSerializer;
    private final CvVersionDiffer versionDiffer;

    public CvVersionService(CvRepository cvRepository, CvVersionRepository versionRepository,
            CvContentService contentService, CvVersionSnapshotSerializer snapshotSerializer) {
        this(cvRepository, versionRepository, contentService, snapshotSerializer, new CvVersionDiffer());
    }

    @Autowired
    public CvVersionService(CvRepository cvRepository, CvVersionRepository versionRepository,
            CvContentService contentService, CvVersionSnapshotSerializer snapshotSerializer,
            CvVersionDiffer versionDiffer) {
        this.cvRepository = cvRepository;
        this.versionRepository = versionRepository;
        this.contentService = contentService;
        this.snapshotSerializer = snapshotSerializer;
        this.versionDiffer = versionDiffer;
    }

    public CvVersionResponse create(UUID cvId, String description) {
        Cv cv = getCvForUpdate(cvId);
        CvContent content = contentService.get(cvId);
        CvVersion parent = versionRepository.findTopByCv_IdOrderByVersionNumberDesc(cvId).orElse(null);
        return saveVersion(cv, description, snapshotSerializer.serialize(cv, content), parent);
    }

    public CvVersionResponse create(UUID cvId, String description, UUID parentVersionId) {
        Cv cv = getCvForUpdate(cvId);
        CvContent content = contentService.get(cvId);
        CvVersion parent = parentVersionId == null ? null : versionRepository.findById(parentVersionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent CV version not found"));
        return saveVersion(cv, description, snapshotSerializer.serialize(cv, content), parent);
    }

    @Transactional(readOnly = true)
    public List<CvVersionResponse> findAll(UUID cvId) {
        getCv(cvId);
        return versionRepository.findAllByCv_IdOrderByVersionNumberDesc(cvId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CvVersionDetailResponse find(UUID cvId, int versionNumber) {
        CvVersion version = versionRepository.findByCv_IdAndVersionNumber(cvId, versionNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV version not found"));
        return new CvVersionDetailResponse(version.getId(), cvId, version.getVersionNumber(),
                version.getDescription(), version.getCreatedAt(), snapshotSerializer.deserialize(version.getSnapshot()),
                parentVersionId(version), parentCvId(version), parentVersionNumber(version));
    }

    @Transactional(readOnly = true)
    public CvVersionDiffResponse diff(UUID cvId, int fromVersionNumber, int toVersionNumber) {
        CvVersion from = versionRepository.findByCv_IdAndVersionNumber(cvId, fromVersionNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV version not found"));
        CvVersion to = versionRepository.findByCv_IdAndVersionNumber(cvId, toVersionNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV version not found"));
        return new CvVersionDiffResponse(toResponse(from), toResponse(to),
                versionDiffer.diff(from.getSnapshot(), to.getSnapshot()));
    }

    public CvVersionResponse restore(UUID cvId, int versionNumber, String description) {
        Cv cv = getCvForUpdate(cvId);
        CvVersion source = versionRepository.findByCv_IdAndVersionNumber(cvId, versionNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV version not found"));
        CvVersionSnapshot snapshot = snapshotSerializer.deserialize(source.getSnapshot());

        cv.setName(snapshot.name());
        cv.setDescription(snapshot.description());
        cv.setLanguage(snapshot.language());
        cv.setStatus(snapshot.status());
        cv.setTemplateId(snapshot.templateId());
        contentService.replace(cvId, snapshot.content());

        String restoreDescription = description == null || description.isBlank()
                ? "Restored from version " + versionNumber
                : description;
        return saveVersion(cv, restoreDescription, snapshotSerializer.serialize(cv, snapshot.content()), source);
    }

    private CvVersionResponse saveVersion(Cv cv, String description,
            com.fasterxml.jackson.databind.JsonNode snapshot, CvVersion parent) {
        int nextVersion = versionRepository.findTopByCv_IdOrderByVersionNumberDesc(cv.getId())
                .map(version -> version.getVersionNumber() + 1).orElse(1);
        CvVersion saved = versionRepository.save(new CvVersion(cv, nextVersion, blankToNull(description), snapshot, parent));
        cv.setCurrentVersionId(saved.getId());
        cvRepository.save(cv);
        return toResponse(saved);
    }

    private CvVersionResponse toResponse(CvVersion version) {
        return new CvVersionResponse(version.getId(), version.getCv().getId(), version.getVersionNumber(),
                version.getDescription(), version.getCreatedAt(), parentVersionId(version), parentCvId(version),
                parentVersionNumber(version));
    }

    private UUID parentVersionId(CvVersion version) {
        return version.getParentVersion() == null ? null : version.getParentVersion().getId();
    }

    private UUID parentCvId(CvVersion version) {
        return version.getParentVersion() == null ? null : version.getParentVersion().getCv().getId();
    }

    private Integer parentVersionNumber(CvVersion version) {
        return version.getParentVersion() == null ? null : version.getParentVersion().getVersionNumber();
    }

    private Cv getCv(UUID cvId) {
        return cvRepository.findById(cvId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));
    }

    private Cv getCvForUpdate(UUID cvId) {
        return cvRepository.findByIdForUpdate(cvId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}