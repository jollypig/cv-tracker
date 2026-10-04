package com.example.cv.cv;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class CvShareService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CvRepository cvRepository;
    private final CvShareRepository shareRepository;
    private final CvContentService contentService;
    private final CvVersionSnapshotSerializer snapshotSerializer;
    private final CvTemplateRepository templateRepository;
    private final CvHtmlRenderer htmlRenderer;

    public CvShareService(CvRepository cvRepository, CvShareRepository shareRepository,
            CvContentService contentService, CvVersionSnapshotSerializer snapshotSerializer,
            CvTemplateRepository templateRepository, CvHtmlRenderer htmlRenderer) {
        this.cvRepository = cvRepository;
        this.shareRepository = shareRepository;
        this.contentService = contentService;
        this.snapshotSerializer = snapshotSerializer;
        this.templateRepository = templateRepository;
        this.htmlRenderer = htmlRenderer;
    }

    @Transactional(readOnly = true)
    public CvShareStatus status(UUID cvId) {
        getCv(cvId);
        return shareRepository.findByCv_Id(cvId)
            .map(share -> new CvShareStatus(true, share.getCreatedAt(), share.getViewCount(), share.getLastViewedAt()))
            .orElseGet(() -> new CvShareStatus(false, null, 0, null));
    }

    public CvShareCreated createOrRotate(UUID cvId) {
        Cv cv = getCv(cvId);
        String token = newToken();
        String tokenHash = hash(token);
        CvShare share = shareRepository.findByCv_Id(cvId).orElseGet(() -> new CvShare(cv, tokenHash));
        share.setTokenHash(tokenHash);
        CvShare saved = shareRepository.save(share);
        return new CvShareCreated(token, saved.getCreatedAt());
    }

    public void revoke(UUID cvId) {
        getCv(cvId);
        shareRepository.deleteByCv_Id(cvId);
    }

    @Transactional(readOnly = true)
    public String render(String token) {
        CvShare share = shareRepository.findByTokenHash(hash(token))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shared CV not found"));
        Cv cv = getCv(share.getCv().getId());
        CvVersionSnapshot snapshot = snapshotSerializer.deserialize(
                snapshotSerializer.serialize(cv, contentService.get(cv.getId())));
        String templateKey = cv.getTemplateId() == null ? "modern"
                : templateRepository.findById(cv.getTemplateId()).map(CvTemplate::getTemplateKey).orElse("modern");
        String html = htmlRenderer.render(snapshot, templateKey);
        shareRepository.recordView(share.getId(), Instant.now());
        return html;
    }

    private Cv getCv(UUID cvId) {
        return cvRepository.findById(cvId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}