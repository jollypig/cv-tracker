package com.example.cv.cv;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "cv_template")
public class CvTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100, updatable = false)
    private String name;

    @Column(length = 500, updatable = false)
    private String description;

    @Column(name = "template_key", nullable = false, unique = true, length = 100, updatable = false)
    private String templateKey;

    @Column(nullable = false, updatable = false)
    private int version;

    @Column(nullable = false, updatable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CvTemplate() {
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getTemplateKey() { return templateKey; }
    public int getVersion() { return version; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}