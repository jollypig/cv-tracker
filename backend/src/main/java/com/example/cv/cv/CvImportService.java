package com.example.cv.cv;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CvImportService {

    private final CvService cvService;
    private final CvContentService contentService;

    public CvImportService(CvService cvService, CvContentService contentService) {
        this.cvService = cvService;
        this.contentService = contentService;
    }

    public CvResponse importCv(UUID personId, CvImportRequest request) {
        CvResponse cv = cvService.create(personId, new CvRequest(
                request.name(), request.description(), request.language(), request.status()));
        contentService.replace(cv.id(), request.content());
        return cv;
    }
}