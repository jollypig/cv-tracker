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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CvContentController.class)
@Import(GlobalExceptionHandler.class)
class CvContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CvContentService contentService;

    @Test
    void acceptsAndReturnsStructuredContent() throws Exception {
        UUID cvId = UUID.randomUUID();
        CvContent content = new CvContent("Summary", List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of());
        when(contentService.replace(eq(cvId), any(CvContent.class))).thenReturn(content);

        mockMvc.perform(put("/api/v1/cvs/{cvId}/content", cvId)
                        .contentType("application/json")
                        .content("""
                                {"summary":"Summary","experiences":[],"education":[],"skillGroups":[],
                                "languages":[],"projects":[],"certifications":[],"customSections":[],"sections":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Summary"));
        verify(contentService).replace(eq(cvId), any(CvContent.class));
    }

    @Test
    void validatesNestedRequiredFields() throws Exception {
        mockMvc.perform(put("/api/v1/cvs/{cvId}/content", UUID.randomUUID())
                        .contentType("application/json")
                        .content("""
                                {"experiences":[{"company":" ","position":"Engineer","sortOrder":0,"projects":[]}],
                                "education":[],"skillGroups":[],"languages":[],"projects":[],
                                "certifications":[],"customSections":[],"sections":[]}
                                """))
                .andExpect(status().isBadRequest());
    }
}