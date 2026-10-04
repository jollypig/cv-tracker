package com.example.cv.cv;

import com.example.cv.person.Person;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Transactional
public class CvTemplateSelectionService {

    private final CvRepository cvRepository;
    private final CvTemplateRepository templateRepository;

    public CvTemplateSelectionService(CvRepository cvRepository, CvTemplateRepository templateRepository) {
        this.cvRepository = cvRepository;
        this.templateRepository = templateRepository;
    }

    public CvResponse selectTemplate(UUID cvId, UUID templateId) {
        Cv cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));
        CvTemplate template = templateRepository.findByIdAndActiveTrue(templateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV template not found"));
        cv.setTemplateId(template.getId());
        Cv saved = cvRepository.save(cv);
        Person person = saved.getPerson();
        return new CvResponse(saved.getId(), person.getId(), person.getFirstName() + " " + person.getLastName(),
                saved.getName(), saved.getDescription(), saved.getLanguage(), saved.getStatus(),
                saved.getTemplateId(), saved.getCurrentVersionId(), saved.getTags().stream().sorted().toList(),
                saved.getCreatedAt(), saved.getUpdatedAt());
    }
}