package com.example.cv.importing;

import com.example.cv.common.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CvDocumentImportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CvDocumentImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CvDocumentImportService importService;

    @Test
    void acceptsMultipartUploadAndReturnsStatusResource() throws Exception {
        UUID importId = UUID.randomUUID();
        when(importService.startImport(any())).thenReturn(response(importId));

        mockMvc.perform(multipart("/api/v1/cvs/import")
                        .file("file", "<h1>CV</h1>".getBytes())
                        .contentType("multipart/form-data"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importId").value(importId.toString()))
                .andExpect(jsonPath("$.status").value("NEEDS_REVIEW"))
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/cvs/import/" + importId)));
    }

    @Test
    void returnsTheImportStatusById() throws Exception {
        UUID importId = UUID.randomUUID();
        when(importService.findById(importId)).thenReturn(response(importId));

        mockMvc.perform(get("/api/v1/cvs/import/{importId}", importId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importId").value(importId.toString()))
                .andExpect(jsonPath("$.fileName").value("resume.html"));
    }

            @Test
            void acceptsDraftCorrections() throws Exception {
            UUID importId = UUID.randomUUID();
            when(importService.updateDraft(org.mockito.ArgumentMatchers.eq(importId), any(ParsedCv.class)))
                .thenReturn(response(importId));

            mockMvc.perform(put("/api/v1/cvs/import/{importId}/draft", importId)
                    .contentType("application/json")
                    .content("""
                        {"personalData":null,"professionalSummary":null,"employment":[],"projects":[],
                         "education":[],"languages":[],"skills":[],"warnings":[]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_REVIEW"));
            }

            @Test
            void acceptsExplicitDraftApproval() throws Exception {
            UUID importId = UUID.randomUUID();
            when(importService.approveDraft(org.mockito.ArgumentMatchers.eq(importId), any(CvDraftApprovalRequest.class)))
                .thenReturn(new CvDocumentImportResponse(importId, "resume.html", "text/html", 12,
                    CvImportStatus.APPROVED, null, response(importId).result(), UUID.randomUUID(),
                    Instant.now(), Instant.now()));

            mockMvc.perform(post("/api/v1/cvs/import/{importId}/approve", importId)
                    .contentType("application/json")
                    .content("""
                        {"personId":"d7a87a1d-937a-408d-9894-84173d49fc18","name":"Resume",
                         "language":"en","status":"DRAFT","tags":[]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.cvId").isNotEmpty());
            }

    private CvDocumentImportResponse response(UUID importId) {
        ParsedCv result = new ParsedCv(null, null, List.of(), List.of(), List.of(), List.of(), List.of());
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new CvDocumentImportResponse(importId, "resume.html", "text/html", 12,
            CvImportStatus.NEEDS_REVIEW, null, result, null, now, now);
    }
}