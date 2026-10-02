package com.example.cv.cv;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cv_export")
public class CvExport {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cv_version_id", nullable = false, updatable = false)
    private CvVersion version;

    @Column(name = "file_name", nullable = false, length = 500, updatable = false)
    private String fileName;

    @Column(name = "storage_key", nullable = false, length = 100, updatable = false)
    private String storageKey;

    @Column(name = "file_size", nullable = false, updatable = false)
    private long fileSize;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CvExport() {
    }

    public CvExport(UUID id, CvVersion version, String fileName, String storageKey, long fileSize) {
        this.id = id;
        this.version = version;
        this.fileName = fileName;
        this.storageKey = storageKey;
        this.fileSize = fileSize;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public CvVersion getVersion() { return version; }
    public String getFileName() { return fileName; }
    public String getStorageKey() { return storageKey; }
    public long getFileSize() { return fileSize; }
    public Instant getCreatedAt() { return createdAt; }
}