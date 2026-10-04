package com.example.cv.cv;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CvShareController {

    private final CvShareService shareService;

    public CvShareController(CvShareService shareService) {
        this.shareService = shareService;
    }

    @GetMapping("/cvs/{cvId}/share")
    public CvShareStatus status(@PathVariable UUID cvId) {
        return shareService.status(cvId);
    }

    @PutMapping("/cvs/{cvId}/share")
    public CvShareLink createOrRotate(@PathVariable UUID cvId) {
        CvShareCreated created = shareService.createOrRotate(cvId);
        URI url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/public/cv-shares/{token}").buildAndExpand(created.token()).toUri();
        return new CvShareLink(url.toString(), created.createdAt());
    }

    @DeleteMapping("/cvs/{cvId}/share")
    public ResponseEntity<Void> revoke(@PathVariable UUID cvId) {
        shareService.revoke(cvId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/public/cv-shares/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> render(@PathVariable String token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Robots-Tag", "noindex, nofollow")
                .header("Referrer-Policy", "no-referrer")
                .contentType(MediaType.TEXT_HTML)
                .body(shareService.render(token));
    }

    public record CvShareLink(String url, Instant createdAt) {
    }
}