package com.example.cv.cv;

import com.example.cv.common.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CvSkillController.class)
@Import(GlobalExceptionHandler.class)
class CvSkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CvSkillService skillService;

    @Test
    void createsSkillGroupWithAddressableId() throws Exception {
        UUID cvId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        when(skillService.createGroup(org.mockito.ArgumentMatchers.eq(cvId),
                org.mockito.ArgumentMatchers.any(CvSkillGroupRequest.class)))
                .thenReturn(new CvSkillGroupResponse(groupId, "Backend", 0, List.of()));

        mockMvc.perform(post("/api/v1/cvs/{cvId}/skill-groups", cvId)
                        .contentType("application/json")
                        .content("{\"name\":\"Backend\",\"sortOrder\":0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(groupId.toString()));
        verify(skillService).createGroup(org.mockito.ArgumentMatchers.eq(cvId),
                org.mockito.ArgumentMatchers.any(CvSkillGroupRequest.class));
    }

    @Test
    void rejectsBlankSkillGroupName() throws Exception {
        mockMvc.perform(post("/api/v1/cvs/{cvId}/skill-groups", UUID.randomUUID())
                        .contentType("application/json")
                        .content("{\"name\":\" \",\"sortOrder\":0}"))
                .andExpect(status().isBadRequest());
    }
}