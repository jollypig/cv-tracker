package com.example.cv.cv;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CvContentServiceTest {

    @Test
    void mergesNewEntriesWithoutOverwritingAndIsIdempotent() {
        CvRepository repository = mock(CvRepository.class);
        UUID targetId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Cv target = new Cv(null, "Target", "en", CvStatus.DRAFT);
        Cv source = new Cv(null, "Source", "de", CvStatus.DRAFT);
        when(repository.findById(targetId)).thenReturn(Optional.of(target));
        when(repository.findById(sourceId)).thenReturn(Optional.of(source));
        when(repository.existsByIdAndPerson_Owner_Id(targetId, ownerId)).thenReturn(true);
        when(repository.existsByIdAndPerson_Owner_Id(sourceId, ownerId)).thenReturn(true);
        var service = new CvContentService(repository);
        var start = java.time.LocalDate.of(2020, 1, 1);
        var oldProject = new CvContent.ExperienceProject("Customer", null, "Platform", "Target description",
                true, true, start, null, null, null, null, null, null, 0);
        var newProject = new CvContent.ExperienceProject("Other", null, "Platform", "New description",
                true, true, start.plusYears(1), null, null, null, null, null, null, 0);
        service.replace(targetId, new CvContent("Target summary", List.of(new CvContent.Experience("Company", "Target role",
                null, null, null, start, null, true, "Keep", 0, List.of(oldProject))), List.of(),
                List.of(new CvContent.SkillGroup("Backend", 0, List.of(new CvContent.Skill("Java", "Expert", 0, true)))),
                List.of(new CvContent.Language("English", "Native", null, null, null, 0)), List.of(), List.of(), List.of(), List.of()));
        var existingExperience = target.getExperiences().get(0);
        service.replace(sourceId, new CvContent("Source summary", List.of(
                new CvContent.Experience(" company ", "Source role", null, null, null, start, null, false, "Replace", 0,
                        List.of(oldProject, newProject)),
                new CvContent.Experience("Company", "New role", null, null, null, start.plusYears(2), null, true, null, 1, List.of())),
                List.of(new CvContent.Education("Institute", null, null, null, start, null, false, null, 0)),
                List.of(new CvContent.SkillGroup(" backend ", 0, List.of(new CvContent.Skill(" java ", "Basic", 0, true),
                        new CvContent.Skill("Kotlin", "Advanced", 1, true))),
                        new CvContent.SkillGroup("Other", 1, List.of(new CvContent.Skill("Java", "Basic", 0, true)))),
                List.of(new CvContent.Language(" english ", "Basic", null, null, null, 0),
                        new CvContent.Language("German", "Native", null, null, null, 1)), List.of(),
                List.of(new CvContent.Certification("Certificate", null, null, null, null, null, null, 0)), List.of(), List.of()));

        CvContent merged = service.merge(targetId, List.of(sourceId), ownerId);
        assertThat(merged.summary()).isEqualTo("Target summary");
        assertThat(target.getName()).isEqualTo("Target");
        assertThat(target.getLanguage()).isEqualTo("en");
        assertThat(target.getExperiences()).contains(existingExperience);
        assertThat(merged.experiences()).extracting(CvContent.Experience::position).containsExactly("New role", "Target role");
        assertThat(merged.experiences().get(1).description()).isEqualTo("Keep");
        assertThat(merged.experiences().get(1).projects()).extracting(CvContent.ExperienceProject::company)
                .containsExactly("Other", "Customer");
        assertThat(merged.skillGroups().get(0).skills()).extracting(CvContent.Skill::name).containsExactly("Java", "Kotlin");
        assertThat(merged.skillGroups().get(0).skills().get(0).level()).isEqualTo("Expert");
        assertThat(merged.skillGroups().get(1).skills()).isEmpty();
        assertThat(merged.languages()).extracting(CvContent.Language::level).containsExactly("Native", "Native");
        assertThat(merged.education()).hasSize(1);
        assertThat(merged.certifications()).hasSize(1);
        assertThat(service.merge(targetId, List.of(sourceId, sourceId, targetId), ownerId)).isEqualTo(merged);
        assertThat(source.getExperiences().get(0).getPosition()).isEqualTo("Source role");
    }

    @Test
    void rejectsUnownedSourceBeforeMutatingTarget() {
        CvRepository repository = mock(CvRepository.class);
        UUID targetId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(repository.existsByIdAndPerson_Owner_Id(targetId, ownerId)).thenReturn(true);
        assertThatThrownBy(() -> new CvContentService(repository).merge(targetId, List.of(sourceId), ownerId))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("CV not found");
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void remapsImportedSkillLinksAndPreservesTargetProjectDetails() {
        CvRepository repository = mock(CvRepository.class);
        UUID targetId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Cv target = new Cv(null, "Target", "en", CvStatus.DRAFT);
        Cv source = new Cv(null, "Source", "en", CvStatus.DRAFT);
        when(repository.findById(targetId)).thenReturn(Optional.of(target));
        when(repository.findById(sourceId)).thenReturn(Optional.of(source));
        when(repository.existsByIdAndPerson_Owner_Id(targetId, ownerId)).thenReturn(true);
        when(repository.existsByIdAndPerson_Owner_Id(sourceId, ownerId)).thenReturn(true);
        var service = new CvContentService(repository);
        var date = java.time.LocalDate.of(2020, 1, 1);
        var targetProject = new CvContent.Project("Platform", "Target role", null, null, null, 0, "target-key", date, null, true);
        service.replace(targetId, new CvContent(null, List.of(), List.of(), List.of(), List.of(),
                List.of(targetProject), List.of(), List.of(), List.of()));
        var details = new CvSkillDetails(null, null, null, null, null, null,
                List.of(new CvSkillDetails.ProjectLink("source-key", "Delivered"),
                        new CvSkillDetails.ProjectLink("target-key", "Built")), true);
        service.replace(sourceId, new CvContent(null, List.of(), List.of(),
                List.of(new CvContent.SkillGroup("Backend", 0, List.of(new CvContent.Skill("Java", null, 0, true, details)))),
                List.of(), List.of(new CvContent.Project("Platform", "Source role", null, null, null, 0, "source-key", date, null, false),
                        new CvContent.Project("New project", null, null, null, null, 1, "target-key", date.plusYears(1), null, true)),
                List.of(), List.of(), List.of()));

        CvContent merged = service.merge(targetId, List.of(sourceId), ownerId);
        assertThat(merged.projects().get(1)).isEqualTo(new CvContent.Project("Platform", "Target role", null, null, null,
                1, "target-key", date, null, true));
        String importedKey = merged.projects().get(0).projectKey();
        assertThat(importedKey).isNotEqualTo("target-key");
        assertThat(merged.skillGroups().get(0).skills().get(0).details().linkedProjects())
                .containsExactly(new CvSkillDetails.ProjectLink("target-key", "Delivered"),
                        new CvSkillDetails.ProjectLink(importedKey, "Built"));
        assertThat(service.merge(targetId, List.of(sourceId), ownerId)).isEqualTo(merged);
    }

    @Test
    void retainsSkillMetadataAndProjectKeysAcrossReplacements() {
        CvRepository repository = mock(CvRepository.class);
        UUID id = UUID.randomUUID();
        Cv cv = new Cv(null, "Resume", "en", CvStatus.DRAFT);
        when(repository.findById(id)).thenReturn(Optional.of(cv));
        var details = new CvSkillDetails(null, new java.math.BigDecimal("3.5"), "2024", "2010", "daily",
                "active", List.of(new CvSkillDetails.ProjectLink("stable", "Delivered")), true);
        var project = new CvContent.Project("Platform", null, null, null, null, 0, "stable",
                java.time.LocalDate.of(2020, 1, 1), java.time.LocalDate.of(2024, 1, 1), false);
        var content = new CvContent(null, List.of(), List.of(), List.of(new CvContent.SkillGroup("Backend", 0,
                List.of(new CvContent.Skill("Java", null, 0, true, details)))), List.of(), List.of(project),
                List.of(), List.of(), List.of());
        var service = new CvContentService(repository);
        var saved = service.replace(id, content);
        var replaced = service.replace(id, saved);
        assertThat(replaced.skillGroups().get(0).skills().get(0).details()).isEqualTo(details);
        assertThat(replaced.projects()).containsExactly(project);
        assertThat(replaced.skillProjectPeriods()).hasSize(1);
        assertThat(replaced.includeSkillDetailsInOutput()).isTrue();
    }

    @Test
    void replacesAndReadsCompleteNestedContent() {
        CvRepository repository = mock(CvRepository.class);
        Cv cv = new Cv(null, "Resume", "en", CvStatus.DRAFT);
        when(repository.findById(UUID.fromString("00000000-0000-0000-0000-000000000001")))
                .thenReturn(Optional.of(cv));
        when(repository.save(cv)).thenReturn(cv);
        CvContentService service = new CvContentService(repository);
        CvContent content = new CvContent("Summary", List.of(new CvContent.Experience("Company", "Engineer",
                null, "Contract", "Hybrid", null, null, false, null, 0, List.of(new CvContent.ExperienceProject(null, null,
                "Project", null, false, true, null, null, "Engineer", "Build", "Java", 4, null, 0)))),
                List.of(new CvContent.Education("University", "Bachelor", "Thesis on distributed systems", "Computer Science",
                        null, null, true, null, 0)), List.of(new CvContent.SkillGroup("Backend", 0,
                        List.of(new CvContent.Skill("Java", "Advanced", 0, false),
                                new CvContent.Skill("Kotlin", null, 1, null)))),
                List.of(new CvContent.Language("English", "Fluent (C1)", "Advanced (B2)", "Intermediate (B1)", "Fluent (C1)", 0)), List.of(), List.of(),
                List.of(), List.of(new CvContent.Section(CvSectionType.EXPERIENCE, true, 0)), true, false);

        CvContent saved = service.replace(UUID.fromString("00000000-0000-0000-0000-000000000001"), content);
        CvContent fetched = service.get(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        assertThat(saved.summary()).isEqualTo("Summary");
        assertThat(saved.includeSkillDetailsInOutput()).isTrue();
        assertThat(fetched.includeSkillDetailsInOutput()).isTrue();
        assertThat(saved.includeSkillLevelsInOutput()).isFalse();
        assertThat(fetched.includeSkillLevelsInOutput()).isFalse();
        assertThat(fetched.experiences()).hasSize(1);
        assertThat(fetched.experiences().get(0).employmentType()).isEqualTo("Contract");
        assertThat(fetched.experiences().get(0).employmentLocation()).isEqualTo("Hybrid");
        assertThat(fetched.experiences().get(0).projects()).hasSize(1);
        assertThat(fetched.experiences().get(0).projects().get(0).showProjectName()).isFalse();
        assertThat(fetched.experiences().get(0).projects().get(0).showCustomerCompany()).isTrue();
        assertThat(fetched.skillGroups().get(0).skills().get(0).name()).isEqualTo("Java");
        assertThat(fetched.skillGroups().get(0).skills().get(0).visible()).isFalse();
        assertThat(fetched.skillGroups().get(0).skills().get(1).visible()).isTrue();
        assertThat(fetched.languages().get(0).reading()).isEqualTo("Advanced (B2)");
        assertThat(fetched.languages().get(0).writing()).isEqualTo("Intermediate (B1)");
        assertThat(fetched.languages().get(0).speaking()).isEqualTo("Fluent (C1)");
        assertThat(fetched.education().get(0).diplomaDegreeWork()).isEqualTo("Thesis on distributed systems");
        assertThat(fetched.education().get(0).current()).isTrue();
        assertThat(fetched.sections().get(0).type()).isEqualTo(CvSectionType.EXPERIENCE);
        verify(repository).save(cv);
    }

    @Test
    void reportsMissingCv() {
        CvRepository repository = mock(CvRepository.class);
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new CvContentService(repository).get(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CV not found");
    }

        @Test
        void returnsDefaultSectionConfigurationForNewCv() {
                CvRepository repository = mock(CvRepository.class);
                UUID id = UUID.randomUUID();
                when(repository.findById(id)).thenReturn(Optional.of(new Cv(null, "Resume", "en", CvStatus.DRAFT)));

                CvContent content = new CvContentService(repository).get(id);

                assertThat(content.sections()).extracting(CvContent.Section::type)
                                .containsExactly(CvSectionType.values());
                assertThat(content.sections()).allMatch(CvContent.Section::visible);
        }
}