package com.example.cv.cv;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "cv_version", uniqueConstraints =
        @UniqueConstraint(name = "uq_cv_version_number", columnNames = {"cv_id", "version_number"}))
public class CvVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cv_id", nullable = false, updatable = false)
    private Cv cv;

    @Column(name = "version_number", nullable = false, updatable = false)
    private int versionNumber;

    @Column(length = 500, updatable = false)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false, updatable = false)
    private JsonNode snapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CvVersion() {
    }

    public CvVersion(Cv cv, int versionNumber, String description, JsonNode snapshot) {
        this.cv = cv;
        this.versionNumber = versionNumber;
        this.description = description;
        this.snapshot = snapshot.deepCopy();
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Cv getCv() { return cv; }
    public int getVersionNumber() { return versionNumber; }
    public String getDescription() { return description; }
    public JsonNode getSnapshot() { return snapshot.deepCopy(); }
    public Instant getCreatedAt() { return createdAt; }
}