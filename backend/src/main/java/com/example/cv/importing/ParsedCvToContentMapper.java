package com.example.cv.importing;

import com.example.cv.cv.CvContent;
import com.example.cv.cv.CvSectionType;
import com.example.cv.cv.CvSkillDetails;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ParsedCvToContentMapper {

    private final Validator validator;

    public ParsedCvToContentMapper(Validator validator) {
        this.validator = validator;
    }

    public CvContent toContent(ParsedCv parsedCv) {
        List<CvContent.Experience> experiences = new ArrayList<>();
        for (int index = 0; index < parsedCv.employment().size(); index++) {
            ParsedEmployment employment = parsedCv.employment().get(index);
            String end = value(employment.endDate());
            experiences.add(new CvContent.Experience(required(employment.company(), "employment company"),
                    required(employment.position(), "employment position"), value(employment.location()),
                    allowedEmploymentType(value(employment.employmentType())), null,
                    date(value(employment.startDate()), false), date(end, true), current(end), null, index,
                    employment.projects().stream().map(this::experienceProject).toList()));
        }

        List<CvContent.Education> education = new ArrayList<>();
        for (int index = 0; index < parsedCv.education().size(); index++) {
            ParsedEducation item = parsedCv.education().get(index);
            String end = value(item.endDate());
            education.add(new CvContent.Education(required(item.institution(), "education institution"),
                    value(item.degree()), null, value(item.fieldOfStudy()), date(value(item.startDate()), false),
                    date(end, true), current(end), value(item.description()), index));
        }

        Map<String, List<CvContent.Skill>> skillsByGroup = new LinkedHashMap<>();
        for (ParsedSkill skill : parsedCv.skills()) {
            String name = nonBlank(skill.canonicalName()) ? skill.canonicalName() : required(skill.name(), "skill name");
            String extractedGroup = value(skill.group());
            String group = nonBlank(extractedGroup) ? extractedGroup.strip() : "Skills";
            CvSkillDetails details = skill.yearsOfExperience() == null && skill.lastUsedDate() == null ? null
                    : new CvSkillDetails(skill.yearsOfExperience(), null,
                            skill.lastUsedDate() == null ? null : skill.lastUsedDate().toString(), null, null,
                            null, List.of(), true);
            List<CvContent.Skill> groupedSkills = skillsByGroup.computeIfAbsent(group, ignored -> new ArrayList<>());
            groupedSkills.add(new CvContent.Skill(name, value(skill.level()), groupedSkills.size(), true, details));
        }
        List<CvContent.SkillGroup> skillGroups = new ArrayList<>();
        skillsByGroup.forEach((name, skills) -> skillGroups.add(new CvContent.SkillGroup(name,
                skillGroups.size(), skills)));

        List<CvContent.Language> languages = new ArrayList<>();
        for (int index = 0; index < parsedCv.languages().size(); index++) {
            ParsedLanguage language = parsedCv.languages().get(index);
            languages.add(new CvContent.Language(required(language.name(), "language name"),
                    value(language.proficiency()), null, null, null, index));
        }

        List<CvContent.Project> projects = new ArrayList<>();
        for (int index = 0; index < parsedCv.projects().size(); index++) {
            ParsedProject project = parsedCv.projects().get(index);
            String end = value(project.endDate());
            projects.add(new CvContent.Project(required(project.projectName(), "project name"),
                    value(project.position()), value(project.projectDescription()),
                    join(project.technologiesAndTools()), null, index, null,
                    date(value(project.startDate()), false), date(end, true), current(end)));
        }

        List<CvContent.Section> sections = java.util.Arrays.stream(CvSectionType.values())
                .map(type -> new CvContent.Section(type, true, type.ordinal())).toList();
        CvContent content = new CvContent(value(parsedCv.professionalSummary()), experiences, education, skillGroups,
            languages, projects, List.of(), List.of(), sections);
        Set<ConstraintViolation<CvContent>> violations = validator.validate(content);
        if (!violations.isEmpty()) {
            String details = violations.stream().map(ConstraintViolation::getMessage).sorted()
                .collect(Collectors.joining("; "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Correct the CV draft before approval: " + details);
        }
        return content;
    }

    private CvContent.ExperienceProject experienceProject(ParsedProject project) {
        String end = value(project.endDate());
        return new CvContent.ExperienceProject(value(project.company()), join(project.industries()),
                required(project.projectName(), "employment project name"), value(project.projectDescription()),
                true, true, date(value(project.startDate()), false), date(end, true), value(project.position()),
                join(project.responsibilities()), join(project.technologiesAndTools()), null, null, 0, null);
    }

    private String required(ExtractedValue<String> extracted, String field) {
        String text = value(extracted);
        if (!nonBlank(text)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Correct or remove the draft entry with a missing " + field + " before approval");
        }
        return text.strip();
    }

    private String value(ExtractedValue<String> extracted) {
        return extracted == null ? null : extracted.value();
    }

    private String join(List<ExtractedValue<String>> values) {
        return values.stream().map(this::value).filter(ParsedCvToContentMapper::nonBlank)
                .map(String::strip).distinct().collect(Collectors.joining(", "));
    }

    private String allowedEmploymentType(String value) {
        return value != null && List.of("Full-time", "Part-time", "Contract", "Freelance", "Internship",
                "Self-employed").contains(value) ? value : null;
    }

    private LocalDate date(String value, boolean endDate) {
        if (!nonBlank(value) || current(value)) {
            return null;
        }
        try {
            if (value.matches("\\d{4}")) {
                return LocalDate.of(Integer.parseInt(value), endDate ? 12 : 1, endDate ? 31 : 1);
            }
            if (value.matches("\\d{4}-\\d{2}")) {
                YearMonth month = YearMonth.parse(value);
                return endDate ? month.atEndOfMonth() : month.atDay(1);
            }
            return LocalDate.parse(value);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Correct the invalid date " + value + " before approval");
        }
    }

    private boolean current(String value) {
        return value != null && List.of("present", "current", "ongoing", "now")
                .contains(value.strip().toLowerCase(Locale.ROOT));
    }

    private static boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }
}