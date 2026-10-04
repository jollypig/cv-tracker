package com.example.cv.cv;

import com.example.cv.person.Person;
import com.example.cv.person.PersonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CvService {

    private final CvRepository cvRepository;
    private final PersonRepository personRepository;

    public CvService(CvRepository cvRepository, PersonRepository personRepository) {
        this.cvRepository = cvRepository;
        this.personRepository = personRepository;
    }

    @Transactional(readOnly = true)
    public List<CvResponse> findAll(UUID personId) {
        List<Cv> cvs;
        if (personId == null) {
            cvs = cvRepository.findAllByOrderByUpdatedAtDesc();
        } else {
            getPerson(personId);
            cvs = cvRepository.findAllByPersonIdOrderByUpdatedAtDesc(personId);
        }
        return cvs.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CvResponse> findAll(UUID personId, UUID ownerId) {
        List<Cv> cvs;
        if (personId == null) {
            cvs = cvRepository.findAllByPerson_Owner_IdOrderByUpdatedAtDesc(ownerId);
        } else {
            personRepository.findByIdAndOwner_Id(personId, ownerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));
            cvs = cvRepository.findAllByPerson_IdAndPerson_Owner_IdOrderByUpdatedAtDesc(personId, ownerId);
        }
        return cvs.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CvResponse findById(UUID id) {
        return toResponse(getCv(id));
    }

    public CvResponse create(UUID personId, CvRequest request) {
        Person person = getPerson(personId);
        Cv cv = new Cv(person, request.name().trim(), request.language().trim(), request.status());
        cv.setDescription(blankToNull(request.description()));
        cv.replaceTags(request.tags());
        return toResponse(cvRepository.save(cv));
    }

    public CvResponse update(UUID id, CvRequest request) {
        Cv cv = getCv(id);
        cv.setName(request.name().trim());
        cv.setDescription(blankToNull(request.description()));
        cv.setLanguage(request.language().trim());
        cv.setStatus(request.status());
        if (request.tags() != null) {
            cv.replaceTags(request.tags());
        }
        return toResponse(cvRepository.save(cv));
    }

    public void delete(UUID id) {
        cvRepository.delete(getCv(id));
    }

    private Person getPerson(UUID id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));
    }

    private Cv getCv(UUID id) {
        return cvRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));
    }

    private CvResponse toResponse(Cv cv) {
        Person person = cv.getPerson();
        return new CvResponse(cv.getId(), person.getId(), person.getFirstName() + " " + person.getLastName(),
                cv.getName(), cv.getDescription(), cv.getLanguage(), cv.getStatus(), cv.getTemplateId(),
            cv.getCurrentVersionId(), cv.getTags().stream().sorted().toList(), cv.getCreatedAt(), cv.getUpdatedAt());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}