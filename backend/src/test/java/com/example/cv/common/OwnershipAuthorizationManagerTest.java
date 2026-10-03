package com.example.cv.common;

import com.example.cv.auth.AuthenticatedUser;
import com.example.cv.auth.AuthenticatedUserRepository;
import com.example.cv.cv.CvExportRepository;
import com.example.cv.cv.CvRepository;
import com.example.cv.cv.CvVersionRepository;
import com.example.cv.person.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnershipAuthorizationManagerTest {

    private static final String ISSUER = "https://issuer.example.org";
    private static final String SUBJECT = "user-123";

    @Mock
    private AuthenticatedUserRepository userRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private CvRepository cvRepository;
    @Mock
    private CvVersionRepository versionRepository;
    @Mock
    private CvExportRepository exportRepository;

    @Test
    void personRequestsMustBelongToTheAuthenticatedAccount() {
        UUID ownerId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        stubAccount(ownerId);
        when(personRepository.existsByIdAndOwner_Id(personId, ownerId)).thenReturn(true, false);
        OwnershipAuthorizationManager manager = manager();

        assertTrue(authorize(manager, "/api/v1/persons/" + personId));
        assertFalse(authorize(manager, "/api/v1/persons/" + personId));
    }

    @Test
    void cvAndNestedVersionRequestsMustBelongToTheAuthenticatedAccount() {
        UUID ownerId = UUID.randomUUID();
        UUID cvId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        stubAccount(ownerId);
        when(cvRepository.existsByIdAndPerson_Owner_Id(cvId, ownerId)).thenReturn(false);
        when(versionRepository.existsByIdAndCv_Person_Owner_Id(versionId, ownerId)).thenReturn(true);
        OwnershipAuthorizationManager manager = manager();

        assertFalse(authorize(manager, "/api/v1/cvs/" + cvId));
        assertTrue(authorize(manager, "/api/v1/cv-versions/" + versionId + "/exports/pdf"));
    }

    @Test
    void exportDownloadsMustBelongToTheAuthenticatedAccount() {
        UUID ownerId = UUID.randomUUID();
        UUID exportId = UUID.randomUUID();
        stubAccount(ownerId);
        when(exportRepository.existsByIdAndVersion_Cv_Person_Owner_Id(exportId, ownerId)).thenReturn(false);

        assertFalse(authorize(manager(), "/api/v1/exports/" + exportId + "/download"));
    }

    private OwnershipAuthorizationManager manager() {
        return new OwnershipAuthorizationManager(
                userRepository, personRepository, cvRepository, versionRepository, exportRepository);
    }

    private void stubAccount(UUID ownerId) {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getId()).thenReturn(ownerId);
        when(userRepository.findByIssuerAndSubject(ISSUER, SUBJECT)).thenReturn(Optional.of(user));
    }

    private boolean authorize(OwnershipAuthorizationManager manager, String path) {
        Instant now = Instant.now();
        OidcIdToken token = new OidcIdToken("test-token", now, now.plusSeconds(300),
                Map.of("iss", ISSUER, "sub", SUBJECT));
        DefaultOidcUser principal = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), token, "sub");
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(principal, null, "ROLE_USER");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        return manager.check(() -> authentication, new RequestAuthorizationContext(request)).isGranted();
    }
}