package com.example.cv.cv;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "cv_project")
public class CvProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cv_id", nullable = false)
    private Cv cv;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 255)
    private String role;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String technologies;

    @Column(length = 500)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "project_key", length = 100)
    private String projectKey;

    @Column(name = "period_from")
    private java.time.LocalDate periodFrom;

    @Column(name = "period_to")
    private java.time.LocalDate periodTo;

    @Column(nullable = false)
    private boolean current;

    protected CvProject() {
    }

    public CvProject(Cv cv, String name) {
        this.cv = cv;
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getProjectKey() { return projectKey; }
    public void setProjectKey(String projectKey) { this.projectKey = projectKey; }
    public java.time.LocalDate getPeriodFrom() { return periodFrom; }
    public void setPeriodFrom(java.time.LocalDate periodFrom) { this.periodFrom = periodFrom; }
    public java.time.LocalDate getPeriodTo() { return periodTo; }
    public void setPeriodTo(java.time.LocalDate periodTo) { this.periodTo = periodTo; }
    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }
    public Cv getCv() { return cv; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTechnologies() { return technologies; }
    public void setTechnologies(String technologies) { this.technologies = technologies; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}