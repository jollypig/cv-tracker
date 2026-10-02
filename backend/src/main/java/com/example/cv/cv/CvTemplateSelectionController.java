package com.example.cv.cv;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cvs/{cvId}/template")
public class CvTemplateSelectionController {

    private final CvTemplateSelectionService selectionService;

    public CvTemplateSelectionController(CvTemplateSelectionService selectionService) {
        this.selectionService = selectionService;
    }

    @PutMapping
    public CvResponse selectTemplate(@PathVariable UUID cvId,
            @Valid @RequestBody CvTemplateSelectionRequest request) {
        return selectionService.selectTemplate(cvId, request.templateId());
    }
}