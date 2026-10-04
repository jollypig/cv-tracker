package com.example.cv.importing;

import com.example.cv.auth.AuthenticatedUser;
import com.example.cv.auth.AuthenticatedUserService;
import com.example.cv.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
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

    private static final UUID OWNER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CvDocumentImportService importService;

    @MockitoBean
    private AuthenticatedUserService authenticatedUsers;

    @BeforeEach
    void setUp() {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getId()).thenReturn(OWNER_ID);
        when(authenticatedUsers.synchronize(any())).thenReturn(user);
    }

    @Test
    void acceptsMultipartUploadAndReturnsStatusResource() throws Exception {
        UUID importId = UUID.randomUUID();
        when(importService.startImport(any(MultipartFile.class), eq(OWNER_ID))).thenReturn(response(importId));

        mockMvc.perform(multipart("/api/v1/cvs/import")
                        .file("file", "<h1>CV</h1>".getBytes())
                        .contentType("multipart/form-data")
                        .with(oidcLogin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importId").value(importId.toString()))
                .andExpect(jsonPath("$.status").value("NEEDS_REVIEW"))
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/cvs/import/" + importId)));
    }

    @Test
    void returnsTheImportStatusById() throws Exception {
        UUID importId = UUID.randomUUID();
        when(importService.findById(importId, OWNER_ID)).thenReturn(response(importId));

        mockMvc.perform(get("/api/v1/cvs/import/{importId}", importId).with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importId").value(importId.toString()))
                .andExpect(jsonPath("$.fileName").value("resume.html"));
    }

            @Test
            void acceptsDraftCorrections() throws Exception {
            UUID importId = UUID.randomUUID();
            when(importService.updateDraft(eq(importId), eq(OWNER_ID), any(ParsedCv.class)))
                .thenReturn(response(importId));

            mockMvc.perform(put("/api/v1/cvs/import/{importId}/draft", importId)
                    .with(oidcLogin())
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
            when(importService.approveDraft(eq(importId), eq(OWNER_ID), any(CvDraftApprovalRequest.class)))
                .thenReturn(new CvDocumentImportResponse(importId, "resume.html", "text/html", 12,
                    CvImportStatus.APPROVED, null, response(importId).result(), UUID.randomUUID(),
                    Instant.now(), Instant.now()));

            mockMvc.perform(post("/api/v1/cvs/import/{importId}/approve", importId)
                    .with(oidcLogin())
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