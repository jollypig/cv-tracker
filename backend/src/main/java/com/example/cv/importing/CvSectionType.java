package com.example.cv.importing;

import java.util.Locale;

public enum CvSectionType {
    PROFILE,
    EXPERIENCE,
    EDUCATION,
    PROJECTS,
    SKILLS,
    LANGUAGES,
    CERTIFICATIONS,
    UNKNOWN;

    public static CvSectionType fromName(String name) {
        if (name == null || name.isBlank()) {
            return UNKNOWN;
        }

        String normalized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return switch (normalized) {
            case "profile", "about", "aboutme", "summary", "professionalsummary", "objective" -> PROFILE;
            case "experience", "workexperience", "workhistory", "professionalexperience", "employmenthistory", "careerhistory" -> EXPERIENCE;
            case "education", "educationhistory", "academicbackground" -> EDUCATION;
            case "projects", "personalprojects", "projectexperience" -> PROJECTS;
            case "skills", "technicalskills", "keyskills", "competencies" -> SKILLS;
            case "languages", "language", "languageproficiency" -> LANGUAGES;
            case "certifications", "certificates", "licenses" -> CERTIFICATIONS;
            default -> UNKNOWN;
        };
    }
}