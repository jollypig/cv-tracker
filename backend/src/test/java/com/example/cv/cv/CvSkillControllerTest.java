package com.example.cv.cv;

import com.example.cv.common.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CvSkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CvSkillService skillService;

    @Test
    void acceptsAndReturnsSkillMetadata() throws Exception {
        UUID cvId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        var details = new CvSkillDetails(new java.math.BigDecimal("4.5"), null, "2024", "2010", "daily",
                "active", List.of(new CvSkillDetails.ProjectLink("project", "Delivered")), true);
        when(skillService.createSkill(org.mockito.ArgumentMatchers.eq(cvId), org.mockito.ArgumentMatchers.eq(groupId),
                org.mockito.ArgumentMatchers.any(CvSkillRequest.class)))
                .thenReturn(new CvSkillResponse(UUID.randomUUID(), "Java", null, 0, true, details));
        mockMvc.perform(post("/api/v1/cvs/{cvId}/skill-groups/{groupId}/skills", cvId, groupId)
                        .contentType("application/json")
                        .content("""
                                {"name":"Java","sortOrder":0,"details":{"yearsOfExperience":4.5,
                                "lastUsed":"2024","startedFrom":"2010","frequency":"daily","status":"active",
                                "linkedProjects":[{"projectKey":"project","outcome":"Delivered"}],"includeInOutput":true}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.details.yearsOfExperience").value(4.5))
                .andExpect(jsonPath("$.details.linkedProjects[0].outcome").value("Delivered"));
        verify(skillService).createSkill(cvId, groupId, new CvSkillRequest("Java", null, 0, null, details));
    }

    @Test
    void rejectsInvalidMetadata() throws Exception {
        for (String details : List.of("{\"yearsOfExperience\":-1}", "{\"yearsActivelyUsed\":-1}",
                "{\"lastUsed\":\"2024-02-30\"}", "{\"startedFrom\":\"2025\",\"lastUsed\":\"2020\"}",
                "{\"frequency\":\"sometimes\"}", "{\"status\":\"unknown\"}", "{\"linkedProjects\":[null]}")) {
            mockMvc.perform(post("/api/v1/cvs/{cvId}/skill-groups/{groupId}/skills", UUID.randomUUID(), UUID.randomUUID())
                            .contentType("application/json")
                            .content("{\"name\":\"Java\",\"sortOrder\":0,\"details\":" + details + "}"))
                    .andExpect(status().isBadRequest());
        }
    }

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