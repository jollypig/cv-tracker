package com.example.cv.cv;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CvShareServiceTest {

    private final CvRepository cvRepository = mock(CvRepository.class);
    private final CvShareRepository shareRepository = mock(CvShareRepository.class);
    private final CvContentService contentService = mock(CvContentService.class);
    private final CvVersionSnapshotSerializer snapshotSerializer = mock(CvVersionSnapshotSerializer.class);
    private final CvHtmlRenderer htmlRenderer = mock(CvHtmlRenderer.class);
    private final CvShareService service = new CvShareService(cvRepository, shareRepository, contentService,
            snapshotSerializer, mock(CvTemplateRepository.class), htmlRenderer);

    @Test
    void createsOpaqueTokenAndStoresOnlyItsHash() {
        UUID cvId = UUID.randomUUID();
        Cv cv = mock(Cv.class);
        when(cvRepository.findById(cvId)).thenReturn(Optional.of(cv));
        when(shareRepository.save(any(CvShare.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CvShareCreated created = service.createOrRotate(cvId);

        assertThat(created.token()).hasSize(43);
        assertThat(created.token()).doesNotContain("=");
        org.mockito.ArgumentCaptor<CvShare> savedShare = org.mockito.ArgumentCaptor.forClass(CvShare.class);
        verify(shareRepository).save(savedShare.capture());
        assertThat(savedShare.getValue().getTokenHash()).isEqualTo(hash(created.token()));
        assertThat(savedShare.getValue().getTokenHash()).isNotEqualTo(created.token());
    }

    @Test
    void rendersTheCurrentCvForAValidShareToken() {
        UUID cvId = UUID.randomUUID();
        String token = "share-token";
        Cv cv = mock(Cv.class);
        CvContent content = mock(CvContent.class);
        CvVersionSnapshot snapshot = mock(CvVersionSnapshot.class);
        JsonNode serialized = mock(JsonNode.class);
        when(cv.getId()).thenReturn(cvId);
        when(shareRepository.findByTokenHash(hash(token))).thenReturn(Optional.of(new CvShare(cv, hash(token))));
        when(cvRepository.findById(cvId)).thenReturn(Optional.of(cv));
        when(contentService.get(cvId)).thenReturn(content);
        when(snapshotSerializer.serialize(cv, content)).thenReturn(serialized);
        when(snapshotSerializer.deserialize(serialized)).thenReturn(snapshot);
        when(htmlRenderer.render(snapshot, "modern")).thenReturn("<html>published CV</html>");

        assertThat(service.render(token)).isEqualTo("<html>published CV</html>");
        verify(htmlRenderer).render(snapshot, "modern");
    }

    @Test
    void unknownShareTokenIsNotFound() {
        when(shareRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.render("unknown"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }
}