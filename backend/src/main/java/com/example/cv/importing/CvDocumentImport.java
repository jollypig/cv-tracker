package com.example.cv.importing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CvDocumentImport() {
    }

    public CvDocumentImport(String fileName, String mediaType, long fileSize) {
        Instant now = Instant.now();
        this.id = UUID.randomUUID();
        this.fileName = fileName;
        this.mediaType = mediaType;
        this.fileSize = fileSize;
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

    public void complete(String resultJson, boolean needsReview) {
        this.resultJson = resultJson;
        this.status = needsReview ? CvImportStatus.NEEDS_REVIEW : CvImportStatus.COMPLETED;
        this.errorMessage = null;
        updatedAt = Instant.now();
    }

    public void fail(String errorMessage) {
        this.status = CvImportStatus.FAILED;
        this.errorMessage = errorMessage;
        this.resultJson = null;
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getFileName() { return fileName; }
    public String getMediaType() { return mediaType; }
    public long getFileSize() { return fileSize; }
    public CvImportStatus getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public String getResultJson() { return resultJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}