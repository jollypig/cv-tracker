package com.example.cv.common;

import com.example.cv.auth.AuthenticatedUserRepository;
import com.example.cv.cv.CvExportRepository;
import com.example.cv.cv.CvRepository;
import com.example.cv.cv.CvVersionRepository;
import com.example.cv.person.PersonRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class OwnershipAuthorizationManager {

    private final AuthenticatedUserRepository userRepository;
    private final PersonRepository personRepository;
    private final CvRepository cvRepository;
    private final CvVersionRepository versionRepository;
    private final CvExportRepository exportRepository;

    public OwnershipAuthorizationManager(
            AuthenticatedUserRepository userRepository,
            PersonRepository personRepository,
            CvRepository cvRepository,
            CvVersionRepository versionRepository,
            CvExportRepository exportRepository) {
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.cvRepository = cvRepository;
        this.versionRepository = versionRepository;
        this.exportRepository = exportRepository;
    }

    public AuthorizationDecision check(
            Supplier<Authentication> authenticationSupplier,
            RequestAuthorizationContext context) {
        Authentication authentication = authenticationSupplier.get();
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser principal)) {
            return new AuthorizationDecision(false);
        }

        Optional<UUID> ownerId = userRepository.findByIssuerAndSubject(
                        principal.getIssuer().toString(), principal.getSubject())
                .map(user -> user.getId());
        boolean allowed = ownerId.map(id -> ownsRequest(context.getRequest(), id)).orElse(false);
        return new AuthorizationDecision(allowed);
    }

    private boolean ownsRequest(HttpServletRequest request, UUID ownerId) {
        String[] path = request.getRequestURI().split("/");
        if (path.length < 4) {
            return false;
        }

        return switch (path[3]) {
            case "persons" -> path.length > 4 && parseUuid(path[4])
                    .map(id -> personRepository.existsByIdAndOwner_Id(id, ownerId)).orElse(false);
            case "cvs" -> cvRequestIsOwned(request, path, ownerId);
            case "cv-versions" -> path.length > 4 && parseUuid(path[4])
                    .map(id -> versionRepository.existsByIdAndCv_Person_Owner_Id(id, ownerId)).orElse(false);
            case "exports" -> path.length > 4 && parseUuid(path[4])
                    .map(id -> exportRepository.existsByIdAndVersion_Cv_Person_Owner_Id(id, ownerId)).orElse(false);
            default -> false;
        };
    }

    private boolean cvRequestIsOwned(HttpServletRequest request, String[] path, UUID ownerId) {
        if (path.length == 4) {
            String personId = request.getParameter("personId");
            return personId == null || parseUuid(personId)
                    .map(id -> personRepository.existsByIdAndOwner_Id(id, ownerId)).orElse(false);
        }
        return parseUuid(path[4])
                .map(id -> cvRepository.existsByIdAndPerson_Owner_Id(id, ownerId)).orElse(false);
    }

    private Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}