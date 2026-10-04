package com.example.cv.importing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cv_document_import")
public class CvDocumentImport {

    @Id
    private UUID id;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "media_type", nullable = false, length = 100)
    private String mediaType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CvImportStatus status;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "result_json", columnDefinition = "text")
    private String resultJson;

    @Column(name = "ai_model_name", length = 250)
    private String aiModelName;

    @Column(name = "ai_model_version", length = 250)
    private String aiModelVersion;

    @Column(name = "cv_id", unique = true)
    private UUID cvId;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CvDocumentImport() {
    }

    public CvDocumentImport(String fileName, String mediaType, long fileSize, UUID ownerId) {
        Instant now = Instant.now();
        this.id = UUID.randomUUID();
        this.fileName = fileName;
        this.mediaType = mediaType;
        this.fileSize = fileSize;
        this.ownerId = ownerId;
        this.status = CvImportStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void markProcessing() {
        status = CvImportStatus.PROCESSING;
        errorMessage = null;
        updatedAt = Instant.now();
    }

    public void complete(String resultJson) {
        this.resultJson = resultJson;
        this.status = CvImportStatus.NEEDS_REVIEW;
        this.errorMessage = null;
        updatedAt = Instant.now();
    }

    public void updateDraft(String resultJson) {
        assertReviewable();
        this.resultJson = resultJson;
        this.status = CvImportStatus.NEEDS_REVIEW;
        updatedAt = Instant.now();
    }

    public void approve(UUID cvId) {
        assertReviewable();
        this.cvId = cvId;
        this.status = CvImportStatus.APPROVED;
        updatedAt = Instant.now();
    }

    public void assertReviewable() {
        if (resultJson == null || status == CvImportStatus.FAILED || status == CvImportStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CV import is not awaiting review");
        }
    }

    public void fail(String errorMessage) {
        this.status = CvImportStatus.FAILED;
        this.errorMessage = errorMessage;
        this.resultJson = null;
        updatedAt = Instant.now();
    }

    public void recordAiModel(CvAiExtractor.ModelMetadata modelMetadata) {
        this.aiModelName = modelMetadata == null ? "unknown" : modelMetadata.name();
        this.aiModelVersion = modelMetadata == null ? "unknown" : modelMetadata.version();
    }

    public UUID getId() { return id; }
    public String getFileName() { return fileName; }
    public String getMediaType() { return mediaType; }
    public long getFileSize() { return fileSize; }
    public CvImportStatus getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public String getResultJson() { return resultJson; }
    public String getAiModelName() { return aiModelName; }
    public String getAiModelVersion() { return aiModelVersion; }
    public UUID getCvId() { return cvId; }
    public UUID getOwnerId() { return ownerId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}