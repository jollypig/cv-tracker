package com.example.cv.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticatedUserService users;
    private final boolean oidcEnabled;

    public AuthController(AuthenticatedUserService users,
            @Value("${OIDC_ISSUER_URI:}") String issuer,
            @Value("${OIDC_CLIENT_ID:}") String clientId) {
        this.users = users;
        this.oidcEnabled = !issuer.isBlank() && !clientId.isBlank();
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse currentUser(@AuthenticationPrincipal OidcUser principal) {
        AuthenticatedUser user = users.synchronize(principal);
        return new AuthenticatedUserResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }

    @GetMapping("/config")
    public AuthConfigurationResponse configuration() {
        return new AuthConfigurationResponse(oidcEnabled, "/oauth2/authorization/oidc");
    }

    public record AuthenticatedUserResponse(UUID id, String email, String displayName) {
    }

    public record AuthConfigurationResponse(boolean oidcEnabled, String loginPath) {
    }
}