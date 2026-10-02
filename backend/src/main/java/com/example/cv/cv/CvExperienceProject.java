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

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cv_experience_project")
public class CvExperienceProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experience_id", nullable = false)
    private CvExperience experience;

    @Column(length = 255)
    private String company;

    @Column(columnDefinition = "text")
    private String industries;

    @Column(name = "project_name", nullable = false, length = 255)
    private String projectName;

    @Column(name = "project_description", columnDefinition = "text")
    private String projectDescription;

    @Column(name = "show_project_name", nullable = false)
    private boolean showProjectName = true;

    @Column(name = "show_customer_company", nullable = false)
    private boolean showCustomerCompany = true;

    @Column(name = "period_from")
    private LocalDate periodFrom;

    @Column(name = "period_to")
    private LocalDate periodTo;

    @Column(length = 255)
    private String position;

    @Column(columnDefinition = "text")
    private String responsibilities;

    @Column(columnDefinition = "text")
    private String technologies;

    @Column(name = "team_size")
    private Integer teamSize;

    @Column(name = "external_link", length = 1000)
    private String externalLink;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected CvExperienceProject() {
    }

    public CvExperienceProject(CvExperience experience, String projectName) {
        this.experience = experience;
        this.projectName = projectName;
    }

    public UUID getId() { return id; }
    public CvExperience getExperience() { return experience; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getIndustries() { return industries; }
    public void setIndustries(String industries) { this.industries = industries; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getProjectDescription() { return projectDescription; }
    public void setProjectDescription(String projectDescription) { this.projectDescription = projectDescription; }
    public boolean isShowProjectName() { return showProjectName; }
    public void setShowProjectName(boolean showProjectName) { this.showProjectName = showProjectName; }
    public boolean isShowCustomerCompany() { return showCustomerCompany; }
    public void setShowCustomerCompany(boolean showCustomerCompany) { this.showCustomerCompany = showCustomerCompany; }
    public LocalDate getPeriodFrom() { return periodFrom; }
    public void setPeriodFrom(LocalDate periodFrom) { this.periodFrom = periodFrom; }
    public LocalDate getPeriodTo() { return periodTo; }
    public void setPeriodTo(LocalDate periodTo) { this.periodTo = periodTo; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getResponsibilities() { return responsibilities; }
    public void setResponsibilities(String responsibilities) { this.responsibilities = responsibilities; }
    public String getTechnologies() { return technologies; }
    public void setTechnologies(String technologies) { this.technologies = technologies; }
    public Integer getTeamSize() { return teamSize; }
    public void setTeamSize(Integer teamSize) { this.teamSize = teamSize; }
    public String getExternalLink() { return externalLink; }
    public void setExternalLink(String externalLink) { this.externalLink = externalLink; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}