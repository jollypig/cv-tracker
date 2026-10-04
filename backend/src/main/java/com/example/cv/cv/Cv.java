package com.example.cv.cv;

import com.example.cv.person.Person;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "cv")
public class Cv {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 10)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CvStatus status;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "current_version_id")
    private UUID currentVersionId;

    @Column(columnDefinition = "text")
    private String summary;

    @ElementCollection
    @CollectionTable(name = "cv_tag", joinColumns = @JoinColumn(name = "cv_id"))
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new LinkedHashSet<>();

    @OneToMany(mappedBy = "cv", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<CvExperience> experiences = new ArrayList<>();

    @OneToMany(mappedBy = "cv", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<CvEducation> education = new ArrayList<>();

    @OneToMany(mappedBy = "cv", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<CvSkillGroup> skillGroups = new ArrayList<>();

    @OneToMany(mappedBy = "cv", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<CvLanguage> languages = new ArrayList<>();

    @OneToMany(mappedBy = "cv", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<CvProject> projects = new ArrayList<>();

    @OneToMany(mappedBy = "cv", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<CvCertification> certifications = new ArrayList<>();

    @OneToMany(mappedBy = "cv", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<CvCustomSection> customSections = new ArrayList<>();

    @OneToMany(mappedBy = "cv", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<CvSection> sections = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Cv() {
    }

    public Cv(Person person, String name, String language, CvStatus status) {
        this.person = person;
        this.name = name;
        this.language = language;
        this.status = status;
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
    public Person getPerson() { return person; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public CvStatus getStatus() { return status; }
    public void setStatus(CvStatus status) { this.status = status; }
    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }
    public UUID getCurrentVersionId() { return currentVersionId; }
    public void setCurrentVersionId(UUID currentVersionId) { this.currentVersionId = currentVersionId; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Set<String> getTags() { return tags; }
    public List<CvExperience> getExperiences() { return experiences; }
    public List<CvEducation> getEducation() { return education; }
    public List<CvSkillGroup> getSkillGroups() { return skillGroups; }
    public List<CvLanguage> getLanguages() { return languages; }
    public List<CvProject> getProjects() { return projects; }
    public List<CvCertification> getCertifications() { return certifications; }
    public List<CvCustomSection> getCustomSections() { return customSections; }
    public List<CvSection> getSections() { return sections; }

    public void replaceExperiences(List<CvExperience> values) { experiences.clear(); experiences.addAll(values); }
    public void replaceEducation(List<CvEducation> values) { education.clear(); education.addAll(values); }
    public void replaceSkillGroups(List<CvSkillGroup> values) { skillGroups.clear(); skillGroups.addAll(values); }
    public void replaceLanguages(List<CvLanguage> values) { languages.clear(); languages.addAll(values); }
    public void replaceProjects(List<CvProject> values) { projects.clear(); projects.addAll(values); }
    public void replaceCertifications(List<CvCertification> values) { certifications.clear(); certifications.addAll(values); }
    public void replaceCustomSections(List<CvCustomSection> values) { customSections.clear(); customSections.addAll(values); }
    public void replaceSections(List<CvSection> values) { sections.clear(); sections.addAll(values); }
    public void replaceTags(Collection<String> values) {
        tags.clear();
        if (values != null) {
            values.stream().map(String::trim).filter(value -> !value.isEmpty()).forEach(tags::add);
        }
    }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}