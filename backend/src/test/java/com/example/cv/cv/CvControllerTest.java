package com.example.cv.cv;

import com.example.cv.common.CorsConfig;
import com.example.cv.common.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CvController.class)
@Import({GlobalExceptionHandler.class, CorsConfig.class})
class CvControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CvService cvService;

        @MockitoBean
        private CvDuplicationService duplicationService;

    @Test
    void createsCvAndReturnsItsResourceLocation() throws Exception {
        UUID personId = UUID.randomUUID();
        UUID cvId = UUID.randomUUID();
        when(cvService.create(eq(personId), any(CvRequest.class))).thenReturn(cv(cvId, personId));

        mockMvc.perform(post("/api/v1/persons/{personId}/cvs", personId)
                        .contentType("application/json")
                        .content("""
                                {"name":"Backend","description":"Java roles","language":"en","status":"DRAFT"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(cvId.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/cvs/" + cvId)));
    }

    @Test
    void listsAndUpdatesCvs() throws Exception {
        UUID personId = UUID.randomUUID();
        UUID cvId = UUID.randomUUID();
        when(cvService.findAll(personId)).thenReturn(List.of(cv(cvId, personId)));
        when(cvService.update(eq(cvId), any(CvRequest.class))).thenReturn(cv(cvId, personId));

        mockMvc.perform(get("/api/v1/persons/{personId}/cvs", personId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].personId").value(personId.toString()));
        mockMvc.perform(put("/api/v1/cvs/{id}", cvId)
                        .contentType("application/json")
                        .content("{\"name\":\"Backend\",\"language\":\"en\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        verify(cvService).update(eq(cvId), any(CvRequest.class));
    }

    @Test
    void rejectsMissingRequiredFieldsAndUnsupportedStatuses() throws Exception {
        mockMvc.perform(post("/api/v1/persons/{personId}/cvs", UUID.randomUUID())
                        .contentType("application/json")
                        .content("""
                                {"name":" ","language":"en","status":"DRAFT"}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/persons/{personId}/cvs", UUID.randomUUID())
                        .contentType("application/json")
                        .content("{\"name\":\"Backend\",\"language\":\"en\",\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletesCv() throws Exception {
        UUID cvId = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/cvs/{id}", cvId))
                .andExpect(status().isNoContent());
        verify(cvService).delete(cvId);
    }

        @Test
        void duplicatesCvAndReturnsTheNewResourceLocation() throws Exception {
                UUID cvId = UUID.randomUUID();
                UUID copyId = UUID.randomUUID();
                when(duplicationService.duplicate(cvId, "Backend Copy")).thenReturn(cv(copyId, UUID.randomUUID()));

                mockMvc.perform(post("/api/v1/cvs/{id}/duplicate", cvId)
                                                .contentType("application/json").content("{\"name\":\"Backend Copy\"}"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(copyId.toString()))
                                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/cvs/" + copyId)));
                verify(duplicationService).duplicate(cvId, "Backend Copy");
        }

    private CvResponse cv(UUID id, UUID personId) {
        Instant timestamp = Instant.parse("2026-10-01T00:00:00Z");
        return new CvResponse(id, personId, "Ada Lovelace", "Backend", null, "en", CvStatus.DRAFT,
                null, null, timestamp, timestamp);
    }
}