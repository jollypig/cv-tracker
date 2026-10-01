package com.example.cv.cv;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CvSkillServiceTest {

    @Test
    void createsSkillGroupAndReturnsItsIdentifier() {
        CvRepository cvRepository = mock(CvRepository.class);
        CvSkillGroupRepository groupRepository = mock(CvSkillGroupRepository.class);
        CvSkillRepository skillRepository = mock(CvSkillRepository.class);
        UUID cvId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        Cv cv = new Cv(null, "Resume", "en", CvStatus.DRAFT);
        when(cvRepository.findById(cvId)).thenReturn(Optional.of(cv));
        when(groupRepository.save(any(CvSkillGroup.class))).thenAnswer(invocation -> {
            CvSkillGroup group = invocation.getArgument(0);
            ReflectionTestUtils.setField(group, "id", groupId);
            return group;
        });
        when(skillRepository.findBySkillGroupIdOrderBySortOrderAsc(groupId)).thenReturn(List.of());

        CvSkillGroupResponse response = new CvSkillService(cvRepository, groupRepository, skillRepository)
                .createGroup(cvId, new CvSkillGroupRequest(" Backend ", 3));

        assertThat(response.id()).isEqualTo(groupId);
        assertThat(response.name()).isEqualTo("Backend");
        assertThat(response.sortOrder()).isEqualTo(3);
    }

    @Test
    void doesNotAllowAccessToGroupFromAnotherCv() {
        CvRepository cvRepository = mock(CvRepository.class);
        CvSkillGroupRepository groupRepository = mock(CvSkillGroupRepository.class);
        CvSkillRepository skillRepository = mock(CvSkillRepository.class);
        UUID cvId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        when(cvRepository.findById(cvId)).thenReturn(Optional.of(new Cv(null, "Resume", "en", CvStatus.DRAFT)));
        when(groupRepository.findByIdAndCvId(groupId, cvId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new CvSkillService(cvRepository, groupRepository, skillRepository)
                .deleteGroup(cvId, groupId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Skill group not found");
        verifyNoInteractions(skillRepository);
    }
}