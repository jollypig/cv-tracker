package com.example.cv.person;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import com.example.cv.auth.AuthenticatedUser;
import com.example.cv.auth.AuthenticatedUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.junit.jupiter.api.BeforeEach;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PersonController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({com.example.cv.common.GlobalExceptionHandler.class, com.example.cv.common.CorsConfig.class})
class PersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PersonService personService;

        @MockitoBean
        private AuthenticatedUserService authenticatedUsers;

        private final UUID ownerId = UUID.randomUUID();

        @BeforeEach
        void setUp() {
                AuthenticatedUser owner = mock(AuthenticatedUser.class);
                when(owner.getId()).thenReturn(ownerId);
                when(authenticatedUsers.synchronize(nullable(OidcUser.class))).thenReturn(owner);
        }

    @Test
    void createsPersonAndReturnsLocation() throws Exception {
        UUID id = UUID.randomUUID();
        when(personService.create(any(PersonRequest.class), any(AuthenticatedUser.class))).thenReturn(person(id));

        mockMvc.perform(post("/api/v1/persons")
                        .contentType("application/json")
                        .content("""
                                {"firstName":"Ada","lastName":"Lovelace","contacts":[
                                  {"type":"EMAIL","value":"ada@example.com","primary":true,"sortOrder":0}
                                ]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.position").value("Analyst"))
                .andExpect(jsonPath("$.contacts[0].type").value("EMAIL"))
                                                                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/" + id)));
    }

    @Test
                void rejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/persons")
                        .contentType("application/json")
                                                                                                .content("{\"firstName\":\" \",\"lastName\":\"Lovelace\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"));
    }

                @Test
                void rejectsUnknownContactType() throws Exception {
                                mockMvc.perform(post("/api/v1/persons")
                                                                                                .contentType("application/json")
                                                                                                .content("""
                                                                                                                                {"firstName":"Ada","lastName":"Lovelace","contacts":[
                                                                                                                                        {"type":"FAX","value":"123","primary":false,"sortOrder":0}
                                                                                                                                ]}
                                                                                                                                """))
                                                                .andExpect(status().isBadRequest())
                                                                .andExpect(jsonPath("$.detail").value("Request body is invalid"));
                }

    @Test
    void supportsReadUpdateAndDeleteEndpoints() throws Exception {
        UUID id = UUID.randomUUID();
        when(personService.findAll(ownerId)).thenReturn(List.of(person(id)));
        when(personService.findById(id, ownerId)).thenReturn(person(id));
        when(personService.update(eq(id), any(PersonRequest.class), eq(ownerId))).thenReturn(person(id));

        mockMvc.perform(get("/api/v1/persons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Ada"));
        mockMvc.perform(get("/api/v1/persons/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Lovelace"));
        mockMvc.perform(put("/api/v1/persons/{id}", id)
                        .contentType("application/json")
                        .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/persons/{id}", id))
                .andExpect(status().isNoContent());
        verify(personService).delete(id, ownerId);
    }

    @Test
    void readsAndUploadsAnOwnedProfilePhoto() throws Exception {
        UUID id = UUID.randomUUID();
        when(personService.getPhoto(id, ownerId)).thenReturn(new PersonPhoto(new byte[]{1, 2, 3}, "image/png"));
        when(personService.uploadPhoto(eq(id), any(), eq(ownerId))).thenReturn(person(id));

        mockMvc.perform(get("/api/v1/persons/{id}/photo", id))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType("image/png"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .bytes(new byte[]{1, 2, 3}));

        MockMultipartFile image = new MockMultipartFile("file", "profile.png", "image/png", new byte[]{1, 2});
        mockMvc.perform(multipart("/api/v1/persons/{id}/photo", id).file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

        @Test
        void allowsTheLocalFrontendOrigin() throws Exception {
                mockMvc.perform(options("/api/v1/persons")
                                                .header(HttpHeaders.ORIGIN, "http://localhost:5174")
                                                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                                .andExpect(status().isOk())
                                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5174"));
        }

    private PersonResponse person(UUID id) {
        return new PersonResponse(id, "Ada", "Lovelace", null, "Analyst", "Female",
                "Single", "Completed", "London", null,
                List.of(new PersonContactResponse(UUID.randomUUID(), ContactType.EMAIL,
                        "ada@example.com", true, 0)),
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-01T00:00:00Z"));
    }
}