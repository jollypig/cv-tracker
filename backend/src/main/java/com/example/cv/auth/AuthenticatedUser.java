package com.example.cv.auth;

import com.example.cv.person.Person;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "app_user", uniqueConstraints =
        @UniqueConstraint(name = "uq_app_user_issuer_subject", columnNames = {"issuer", "subject"}))
public class AuthenticatedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String issuer;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(length = 320)
    private String email;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "owner")
    private List<Person> people = new ArrayList<>();

    protected AuthenticatedUser() {
    }

    public AuthenticatedUser(String issuer, String subject, String email, String displayName) {
        this.issuer = issuer;
        this.subject = subject;
        updateProfile(email, displayName);
    }

    public void updateProfile(String email, String displayName) {
        this.email = email;
        this.displayName = displayName;
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

    public UUID getId() { return id; }
    public String getIssuer() { return issuer; }
    public String getSubject() { return subject; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
}