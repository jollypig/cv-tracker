package com.example.cv.auth;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticatedUserService {

    private final AuthenticatedUserRepository userRepository;

    public AuthenticatedUserService(AuthenticatedUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public AuthenticatedUser synchronize(OidcUser principal) {
        String issuer = principal.getIssuer().toString();
        String subject = principal.getSubject();
        String email = principal.getEmail();
        String displayName = principal.getFullName();

        AuthenticatedUser user = userRepository.findByIssuerAndSubject(issuer, subject)
                .orElseGet(() -> new AuthenticatedUser(issuer, subject, email, displayName));
        user.updateProfile(email, displayName);
        return userRepository.save(user);
    }
}