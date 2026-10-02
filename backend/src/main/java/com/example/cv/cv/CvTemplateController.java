package com.example.cv.cv;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
public class CvTemplateController {

    private final CvTemplateService templateService;

    public CvTemplateController(CvTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public List<CvTemplateResponse> findAll() {
        return templateService.findAll();
    }

    @GetMapping("/{templateId}")
    public CvTemplateResponse find(@PathVariable UUID templateId) {
        return templateService.find(templateId);
    }
}