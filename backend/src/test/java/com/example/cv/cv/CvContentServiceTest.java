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
                        List.of(new CvContent.Skill("Java", "Advanced", 0)))),
                List.of(new CvContent.Language("English", "Fluent (C1)", "Advanced (B2)", "Intermediate (B1)", "Fluent (C1)", 0)), List.of(), List.of(),
                List.of(), List.of(new CvContent.Section(CvSectionType.EXPERIENCE, true, 0)));

        CvContent saved = service.replace(UUID.fromString("00000000-0000-0000-0000-000000000001"), content);
        CvContent fetched = service.get(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        assertThat(saved.summary()).isEqualTo("Summary");
        assertThat(fetched.experiences()).hasSize(1);
        assertThat(fetched.experiences().get(0).employmentType()).isEqualTo("Contract");
        assertThat(fetched.experiences().get(0).employmentLocation()).isEqualTo("Hybrid");
        assertThat(fetched.experiences().get(0).projects()).hasSize(1);
        assertThat(fetched.experiences().get(0).projects().get(0).showProjectName()).isFalse();
        assertThat(fetched.experiences().get(0).projects().get(0).showCustomerCompany()).isTrue();
        assertThat(fetched.skillGroups().get(0).skills().get(0).name()).isEqualTo("Java");
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