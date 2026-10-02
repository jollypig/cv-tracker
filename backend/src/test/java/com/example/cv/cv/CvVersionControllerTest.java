package com.example.cv.cv;

import com.example.cv.common.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CvVersionController.class)
@Import(GlobalExceptionHandler.class)
class CvVersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CvVersionService versionService;

    @Test
    void createsAVersionWithDescription() throws Exception {
        UUID cvId = UUID.randomUUID();
        when(versionService.create(cvId, "Release"))
                .thenReturn(new CvVersionResponse(UUID.randomUUID(), cvId, 1, "Release", Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(post("/api/v1/cvs/{cvId}/versions", cvId)
                        .contentType("application/json").content("{\"description\":\"Release\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.description").value("Release"));
        verify(versionService).create(cvId, "Release");
    }

    @Test
    void rejectsOverlongVersionDescriptions() throws Exception {
        mockMvc.perform(post("/api/v1/cvs/{cvId}/versions", UUID.randomUUID())
                        .contentType("application/json").content("{\"description\":\"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exposesHistoryAndReadOnlyVersionDetails() throws Exception {
        UUID cvId = UUID.randomUUID();
        CvVersionSnapshot snapshot = new CvVersionSnapshot(null, "Resume", null, "en", CvStatus.DRAFT,
                new CvContent("Saved", java.util.List.of(), java.util.List.of(), java.util.List.of(),
                        java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of()),
                null);
        when(versionService.find(cvId, 1)).thenReturn(new CvVersionDetailResponse(UUID.randomUUID(), cvId, 1,
                "Initial", Instant.parse("2026-01-01T00:00:00Z"), snapshot));

        mockMvc.perform(get("/api/v1/cvs/{cvId}/versions", cvId)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/cvs/{cvId}/versions/1", cvId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshot.content.summary").value("Saved"));
    }

    @Test
    void restoresAVersionAsANewVersion() throws Exception {
        UUID cvId = UUID.randomUUID();
        when(versionService.restore(cvId, 1, "Recovery"))
                .thenReturn(new CvVersionResponse(UUID.randomUUID(), cvId, 2, "Recovery", Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(post("/api/v1/cvs/{cvId}/versions/1/restore", cvId)
                        .contentType("application/json").content("{\"description\":\"Recovery\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNumber").value(2));
        verify(versionService).restore(cvId, 1, "Recovery");
    }
}