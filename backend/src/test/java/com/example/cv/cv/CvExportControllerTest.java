package com.example.cv.cv;

import com.example.cv.common.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CvExportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CvExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CvExportService exportService;

    @Test
    void createsPdfExportForAVersion() throws Exception {
        UUID versionId = UUID.randomUUID();
        when(exportService.exportPdf(versionId)).thenReturn(new CvExportResponse(UUID.randomUUID(), versionId,
                2, "Jane_Doe_Resume_v2.pdf", 4096, Instant.parse("2026-01-01T00:00:00Z"), "/download"));

        mockMvc.perform(post("/api/v1/cv-versions/{versionId}/exports/pdf", versionId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionId").value(versionId.toString()))
                .andExpect(jsonPath("$.fileName").value("Jane_Doe_Resume_v2.pdf"));
        verify(exportService).exportPdf(versionId);
    }

    @Test
    void createsDocxExportForAVersion() throws Exception {
        UUID versionId = UUID.randomUUID();
        when(exportService.exportDocx(versionId)).thenReturn(new CvExportResponse(UUID.randomUUID(), versionId,
                2, "Jane_Doe_Resume_v2.docx", 4096, Instant.parse("2026-01-01T00:00:00Z"), "/download"));

        mockMvc.perform(post("/api/v1/cv-versions/{versionId}/exports/docx", versionId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionId").value(versionId.toString()))
                .andExpect(jsonPath("$.fileName").value("Jane_Doe_Resume_v2.docx"));
        verify(exportService).exportDocx(versionId);
    }

    @Test
    void listsExportHistoryForAVersion() throws Exception {
        UUID versionId = UUID.randomUUID();
        when(exportService.findHistory(versionId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/cv-versions/{versionId}/exports", versionId))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(exportService).findHistory(versionId);
    }

    @Test
    void downloadsPdfWithAttachmentHeaders() throws Exception {
        UUID exportId = UUID.randomUUID();
        byte[] pdf = "%PDF-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        when(exportService.download(exportId)).thenReturn(new CvExportFile("Jane_Doe_Resume_v2.pdf", pdf));

        mockMvc.perform(get("/api/v1/exports/{exportId}/download", exportId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf));
        verify(exportService).download(exportId);
    }

    @Test
    void downloadsDocxWithWordContentTypeAndAttachmentHeaders() throws Exception {
        UUID exportId = UUID.randomUUID();
        byte[] docx = "docx-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        when(exportService.download(exportId)).thenReturn(new CvExportFile("Jane_Doe_Resume_v2.docx", docx,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

        mockMvc.perform(get("/api/v1/exports/{exportId}/download", exportId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .andExpect(content().bytes(docx));
        verify(exportService).download(exportId);
    }
}