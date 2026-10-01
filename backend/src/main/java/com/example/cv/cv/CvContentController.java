package com.example.cv.cv;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cvs/{cvId}/content")
public class CvContentController {

    private final CvContentService contentService;

    public CvContentController(CvContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public CvContent get(@PathVariable UUID cvId) {
        return contentService.get(cvId);
    }

    @PutMapping
    public CvContent replace(@PathVariable UUID cvId, @Valid @RequestBody CvContent content) {
        return contentService.replace(cvId, content);
    }
}