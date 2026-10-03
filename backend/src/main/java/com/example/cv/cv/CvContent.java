package com.example.cv.cv;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CvContent(
        @Size(max = 10000) String summary,
        @NotNull @Size(max = 100) List<@Valid Experience> experiences,
        @NotNull @Size(max = 100) List<@Valid Education> education,
        @NotNull @Size(max = 100) List<@Valid SkillGroup> skillGroups,
        @NotNull @Size(max = 100) List<@Valid Language> languages,
        @NotNull @Size(max = 100) List<@Valid Project> projects,
        @NotNull @Size(max = 100) List<@Valid Certification> certifications,
        @NotNull @Size(max = 100) List<@Valid CustomSection> customSections,
        @NotNull @Size(max = 8) List<@Valid Section> sections) {

    public record Experience(
            @NotBlank @Size(max = 255) String company,
            @NotBlank @Size(max = 255) String position,
            @Size(max = 255) String location,
            @Pattern(regexp = "Full-time|Part-time|Contract|Freelance|Internship|Self-employed") String employmentType,
            @Pattern(regexp = "On-site|Hybrid|Remote") String employmentLocation,
            LocalDate startDate,
            LocalDate endDate,
            boolean current,
            @Size(max = 10000) String description,
            @PositiveOrZero int sortOrder,
            @NotNull @Size(max = 100) List<@Valid ExperienceProject> projects) {
    }

    public record ExperienceProject(
            @Size(max = 255) String company,
            @Size(max = 10000) String industries,
            @NotBlank @Size(max = 255) String projectName,
            @Size(max = 10000) String projectDescription,
            Boolean showProjectName,
            Boolean showCustomerCompany,
            LocalDate periodFrom,
            LocalDate periodTo,
            @Size(max = 255) String position,
            @Size(max = 10000) String responsibilities,
            @Size(max = 10000) String technologies,
            @PositiveOrZero Integer teamSize,
            @Size(max = 1000) String externalLink,
            @PositiveOrZero int sortOrder) {
    }

    public record Education(
            @NotBlank @Size(max = 255) String institution,
            @Size(max = 255) String degree,
            @Size(max = 10000) String diplomaDegreeWork,
            @Size(max = 255) String fieldOfStudy,
            LocalDate startDate,
            LocalDate endDate,
            boolean current,
            @Size(max = 10000) String description,
            @PositiveOrZero int sortOrder) {
    }

    public record SkillGroup(
            @NotBlank @Size(max = 255) String name,
            @PositiveOrZero int sortOrder,
            @NotNull @Size(max = 100) List<@Valid Skill> skills) {
    }

    public record Skill(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 50) String level,
            @PositiveOrZero int sortOrder,
            Boolean visible) {
    }

    public record Language(
            @NotBlank @Size(max = 100) String language,
            @Size(max = 50) String level,
            @Size(max = 50) String reading,
            @Size(max = 50) String writing,
            @Size(max = 50) String speaking,
            @PositiveOrZero int sortOrder) {
    }

    public record Project(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 255) String role,
            @Size(max = 10000) String description,
            @Size(max = 10000) String technologies,
            @Size(max = 500) String url,
            @PositiveOrZero int sortOrder) {
    }

    public record Certification(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 10000) String description,
            @Size(max = 255) String issuer,
            LocalDate issueDate,
            LocalDate expiryDate,
            @Size(max = 255) String credentialId,
            @Size(max = 500) String credentialUrl,
            @PositiveOrZero int sortOrder) {
    }

    public record CustomSection(
            @NotBlank @Size(max = 255) String title,
            @Size(max = 10000) String content,
            @PositiveOrZero int sortOrder) {
    }

    public record Section(
            @NotNull CvSectionType type,
            boolean visible,
            @PositiveOrZero int sortOrder) {
    }
}