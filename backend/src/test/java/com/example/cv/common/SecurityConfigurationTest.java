package com.example.cv.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({SecurityConfigurationTest.ProbeController.class, StatusController.class})
@Import(SecurityConfiguration.class)
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnershipAuthorizationManager ownershipManager;

    @Test
    void apiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/security-probe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void statusEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk());
    }

    @Test
    void ownedResourcesReturnForbiddenWhenOwnershipIsDenied() throws Exception {
        when(ownershipManager.check(any(), any())).thenReturn(new AuthorizationDecision(false));

        mockMvc.perform(get("/api/v1/persons/{id}", UUID.randomUUID()).with(oidcLogin()))
                .andExpect(status().isForbidden());
    }

    @RestController
    static class ProbeController {
        @GetMapping("/api/v1/security-probe")
        String probe() {
            return "ok";
        }

        @GetMapping("/api/v1/persons/{id}")
        String person() {
            return "person";
        }
    }
}