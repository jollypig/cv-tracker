package com.example.cv.importing;

import java.util.List;
import java.util.Objects;

public record ParsedCv(
        PersonalData personalData,
        ExtractedValue<String> professionalSummary,
        List<ParsedEmployment> employment,
        List<ParsedProject> projects,
        List<ParsedEducation> education,
        List<ParsedLanguage> languages,
        List<ParsedSkill> skills,
        List<String> warnings
) {

    public ParsedCv(
            PersonalData personalData,
            ExtractedValue<String> professionalSummary,
            List<ParsedEmployment> employment,
            List<ParsedProject> projects,
            List<ParsedEducation> education,
            List<ParsedLanguage> languages,
            List<ParsedSkill> skills
    ) {
        this(personalData, professionalSummary, employment, projects, education, languages, skills, List.of());
    }

    public ParsedCv {
        Objects.requireNonNull(employment, "employment");
        Objects.requireNonNull(projects, "projects");
        Objects.requireNonNull(education, "education");
        Objects.requireNonNull(languages, "languages");
        Objects.requireNonNull(skills, "skills");
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        employment = List.copyOf(employment);
        projects = List.copyOf(projects);
        education = List.copyOf(education);
        languages = List.copyOf(languages);
        skills = List.copyOf(skills);
    }
}