package com.example.cv.cv;

import com.example.cv.auth.AuthenticatedUserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cvs/{cvId}/content")
public class CvContentController {

    private final CvContentService contentService;
    private final AuthenticatedUserService authenticatedUsers;

    public CvContentController(CvContentService contentService, AuthenticatedUserService authenticatedUsers) {
        this.contentService = contentService;
        this.authenticatedUsers = authenticatedUsers;
    }

    @GetMapping
    public CvContent get(@PathVariable UUID cvId) {
        return contentService.get(cvId);
    }

    @PutMapping
    public CvContent replace(@PathVariable UUID cvId, @Valid @RequestBody CvContent content) {
        return contentService.replace(cvId, content);
    }

    @PostMapping("/merge")
    public CvContent merge(@PathVariable UUID cvId, @Valid @RequestBody CvMergeRequest request,
            @AuthenticationPrincipal OidcUser principal) {
        return contentService.merge(cvId, request.sourceCvIds(), authenticatedUsers.synchronize(principal).getId());
    }
}