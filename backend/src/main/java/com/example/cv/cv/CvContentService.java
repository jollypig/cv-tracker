package com.example.cv.cv;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.function.ToIntFunction;

@Service
@Transactional
public class CvContentService {

    private final CvRepository cvRepository;

    public CvContentService(CvRepository cvRepository) {
        this.cvRepository = cvRepository;
    }

    @Transactional(readOnly = true)
    public CvContent get(UUID cvId) {
        return toContent(getCv(cvId));
    }

    public CvContent replace(UUID cvId, CvContent content) {
        Cv cv = getCv(cvId);
        cv.setSummary(content.summary());
        cv.replaceExperiences(content.experiences().stream().map(item -> experience(cv, item)).toList());
        cv.replaceEducation(content.education().stream().map(item -> education(cv, item)).toList());
        cv.replaceSkillGroups(content.skillGroups().stream().map(item -> skillGroup(cv, item)).toList());
        cv.replaceLanguages(content.languages().stream().map(item -> language(cv, item)).toList());
        cv.replaceProjects(content.projects().stream().map(item -> project(cv, item)).toList());
        cv.replaceCertifications(content.certifications().stream().map(item -> certification(cv, item)).toList());
        cv.replaceCustomSections(content.customSections().stream().map(item -> customSection(cv, item)).toList());
        replaceSections(cv, content.sections());
        cvRepository.save(cv);
        return toContent(cv);
    }

    private CvExperience experience(Cv cv, CvContent.Experience input) {
        CvExperience entity = new CvExperience(cv, input.company().trim(), input.position().trim());
        entity.setLocation(blankToNull(input.location()));
        entity.setEmploymentType(input.employmentType());
        entity.setEmploymentLocation(input.employmentLocation());
        entity.setStartDate(input.startDate());
        entity.setEndDate(input.endDate());
        entity.setCurrent(input.current());
        entity.setDescription(blankToNull(input.description()));
        entity.setSortOrder(input.sortOrder());
        entity.replaceProjects(input.projects().stream().map(item -> experienceProject(entity, item)).toList());
        return entity;
    }

    private CvExperienceProject experienceProject(CvExperience experience, CvContent.ExperienceProject input) {
        CvExperienceProject entity = new CvExperienceProject(experience, input.projectName().trim());
        entity.setProjectKey(input.projectKey());
        entity.setCompany(blankToNull(input.company()));
        entity.setIndustries(blankToNull(input.industries()));
        entity.setProjectDescription(blankToNull(input.projectDescription()));
        entity.setShowProjectName(input.showProjectName() == null || input.showProjectName());
        entity.setShowCustomerCompany(input.showCustomerCompany() == null || input.showCustomerCompany());
        entity.setPeriodFrom(input.periodFrom());
        entity.setPeriodTo(input.periodTo());
        entity.setPosition(blankToNull(input.position()));
        entity.setResponsibilities(blankToNull(input.responsibilities()));
        entity.setTechnologies(blankToNull(input.technologies()));
        entity.setTeamSize(input.teamSize());
        entity.setExternalLink(blankToNull(input.externalLink()));
        entity.setSortOrder(input.sortOrder());
        return entity;
    }

    private CvEducation education(Cv cv, CvContent.Education input) {
        CvEducation entity = new CvEducation(cv, input.institution().trim());
        entity.setDegree(blankToNull(input.degree()));
        entity.setDiplomaDegreeWork(blankToNull(input.diplomaDegreeWork()));
        entity.setFieldOfStudy(blankToNull(input.fieldOfStudy()));
        entity.setStartDate(input.startDate());
        entity.setEndDate(input.endDate());
        entity.setCurrent(input.current());
        entity.setDescription(blankToNull(input.description()));
        entity.setSortOrder(input.sortOrder());
        return entity;
    }

    private CvSkillGroup skillGroup(Cv cv, CvContent.SkillGroup input) {
        CvSkillGroup entity = new CvSkillGroup(cv, input.name().trim());
        entity.setSortOrder(input.sortOrder());
        entity.replaceSkills(input.skills().stream().map(item -> {
            CvSkill skill = new CvSkill(entity, item.name().trim());
            skill.setLevel(blankToNull(item.level()));
            skill.setSortOrder(item.sortOrder());
            skill.setVisible(item.visible() == null || item.visible());
            skill.setDetails(item.details());
            return skill;
        }).toList());
        return entity;
    }

    private CvLanguage language(Cv cv, CvContent.Language input) {
        CvLanguage entity = new CvLanguage(cv, input.language().trim());
        entity.setLevel(blankToNull(input.level()));
        entity.setReading(blankToNull(input.reading()));
        entity.setWriting(blankToNull(input.writing()));
        entity.setSpeaking(blankToNull(input.speaking()));
        entity.setSortOrder(input.sortOrder());
        return entity;
    }

    private CvProject project(Cv cv, CvContent.Project input) {
        CvProject entity = new CvProject(cv, input.name().trim());
        entity.setProjectKey(input.projectKey());
        entity.setPeriodFrom(input.periodFrom());
        entity.setPeriodTo(input.periodTo());
        entity.setCurrent(input.current());
        entity.setRole(blankToNull(input.role()));
        entity.setDescription(blankToNull(input.description()));
        entity.setTechnologies(blankToNull(input.technologies()));
        entity.setUrl(blankToNull(input.url()));
        entity.setSortOrder(input.sortOrder());
        return entity;
    }

    private CvCertification certification(Cv cv, CvContent.Certification input) {
        CvCertification entity = new CvCertification(cv, input.name().trim());
        entity.setDescription(blankToNull(input.description()));
        entity.setIssuer(blankToNull(input.issuer()));
        entity.setIssueDate(input.issueDate());
        entity.setExpiryDate(input.expiryDate());
        entity.setCredentialId(blankToNull(input.credentialId()));
        entity.setCredentialUrl(blankToNull(input.credentialUrl()));
        entity.setSortOrder(input.sortOrder());
        return entity;
    }

    private CvCustomSection customSection(Cv cv, CvContent.CustomSection input) {
        CvCustomSection entity = new CvCustomSection(cv, input.title().trim());
        entity.setContent(blankToNull(input.content()));
        entity.setSortOrder(input.sortOrder());
        return entity;
    }

    private void replaceSections(Cv cv, List<CvContent.Section> inputs) {
        Set<CvSectionType> types = new java.util.HashSet<>();
        if (inputs.stream().anyMatch(input -> !types.add(input.type()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section types must be unique");
        }

        Map<CvSectionType, CvSection> existing = new EnumMap<>(CvSectionType.class);
        cv.getSections().forEach(section -> existing.put(section.getSectionType(), section));
        List<CvSection> replacement = inputs.stream().map(input -> {
            CvSection section = existing.remove(input.type());
            if (section == null) {
                section = new CvSection(cv, input.type());
            }
            section.setVisible(input.visible());
            section.setSortOrder(input.sortOrder());
            return section;
        }).toList();
        cv.replaceSections(replacement);
    }

    private CvContent toContent(Cv cv) {
        List<CvContent.Section> sections = ordered(cv.getSections(), CvSection::getSortOrder).stream()
            .map(item -> new CvContent.Section(item.getSectionType(), item.isVisible(), item.getSortOrder()))
            .toList();
        if (sections.isEmpty()) {
            sections = Arrays.stream(CvSectionType.values())
                .map(type -> new CvContent.Section(type, true, type.ordinal()))
                .toList();
        }
        return new CvContent(cv.getSummary(),
                ordered(cv.getExperiences(), CvExperience::getSortOrder).stream().map(item -> new CvContent.Experience(item.getCompany(), item.getPosition(),
                    item.getLocation(), item.getEmploymentType(), item.getEmploymentLocation(), item.getStartDate(), item.getEndDate(), item.isCurrent(), item.getDescription(),
                    item.getSortOrder(), ordered(item.getProjects(), CvExperienceProject::getSortOrder).stream().map(project -> new CvContent.ExperienceProject(
                                project.getCompany(), project.getIndustries(), project.getProjectName(),
                                project.getProjectDescription(), project.isShowProjectName(), project.isShowCustomerCompany(),
                                project.getPeriodFrom(), project.getPeriodTo(),
                                project.getPosition(), project.getResponsibilities(), project.getTechnologies(),
                                project.getTeamSize(), project.getExternalLink(), project.getSortOrder(), project.getProjectKey())).toList())).toList(),
                ordered(cv.getEducation(), CvEducation::getSortOrder).stream().map(item -> new CvContent.Education(item.getInstitution(), item.getDegree(),
                    item.getDiplomaDegreeWork(), item.getFieldOfStudy(), item.getStartDate(), item.getEndDate(), item.isCurrent(), item.getDescription(),
                        item.getSortOrder())).toList(),
                ordered(cv.getSkillGroups(), CvSkillGroup::getSortOrder).stream().map(item -> new CvContent.SkillGroup(item.getName(), item.getSortOrder(),
                    ordered(item.getSkills(), CvSkill::getSortOrder).stream().map(skill -> new CvContent.Skill(skill.getName(), skill.getLevel(),
                                skill.getSortOrder(), skill.isVisible(), skill.getDetails())).toList())).toList(),
                ordered(cv.getLanguages(), CvLanguage::getSortOrder).stream().map(item -> new CvContent.Language(item.getLanguage(), item.getLevel(),
                    item.getReading(), item.getWriting(), item.getSpeaking(), item.getSortOrder())).toList(),
                ordered(cv.getProjects(), CvProject::getSortOrder).stream().map(item -> new CvContent.Project(item.getName(), item.getRole(),
                        item.getDescription(), item.getTechnologies(), item.getUrl(), item.getSortOrder(), item.getProjectKey(),
                        item.getPeriodFrom(), item.getPeriodTo(), item.isCurrent())).toList(),
                ordered(cv.getCertifications(), CvCertification::getSortOrder).stream().map(item -> new CvContent.Certification(item.getName(), item.getDescription(), item.getIssuer(),
                        item.getIssueDate(), item.getExpiryDate(), item.getCredentialId(), item.getCredentialUrl(),
                        item.getSortOrder())).toList(),
                ordered(cv.getCustomSections(), CvCustomSection::getSortOrder).stream().map(item -> new CvContent.CustomSection(item.getTitle(), item.getContent(),
                        item.getSortOrder())).toList(),
                sections);
    }

    private Cv getCv(UUID cvId) {
        return cvRepository.findById(cvId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));
    }

    private <T> List<T> ordered(List<T> values, ToIntFunction<T> sortOrder) {
        return values.stream().sorted(Comparator.comparingInt(sortOrder)).toList();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}