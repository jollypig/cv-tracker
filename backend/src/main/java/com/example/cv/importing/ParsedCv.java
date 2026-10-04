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
        List<ParsedSkill> skills
) {

    public ParsedCv {
        Objects.requireNonNull(employment, "employment");
        Objects.requireNonNull(projects, "projects");
        Objects.requireNonNull(education, "education");
        Objects.requireNonNull(languages, "languages");
        Objects.requireNonNull(skills, "skills");
        employment = List.copyOf(employment);
        projects = List.copyOf(projects);
        education = List.copyOf(education);
        languages = List.copyOf(languages);
        skills = List.copyOf(skills);
    }
}