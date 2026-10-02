package com.example.cv.cv;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class CvTemplateService {

    private final CvTemplateRepository templateRepository;

    public CvTemplateService(CvTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public List<CvTemplateResponse> findAll() {
        return templateRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(CvTemplateResponse::from)
                .toList();
    }

    public CvTemplateResponse find(UUID id) {
        return templateRepository.findByIdAndActiveTrue(id)
                .map(CvTemplateResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV template not found"));
    }
}