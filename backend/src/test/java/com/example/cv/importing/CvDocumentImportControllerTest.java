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
                .andExpect(jsonPath("$.status").value("COMPLETED"))
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

    private CvDocumentImportResponse response(UUID importId) {
        ParsedCv result = new ParsedCv(null, null, List.of(), List.of(), List.of(), List.of(), List.of());
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new CvDocumentImportResponse(importId, "resume.html", "text/html", 12,
                CvImportStatus.COMPLETED, null, result, now, now);
    }
}