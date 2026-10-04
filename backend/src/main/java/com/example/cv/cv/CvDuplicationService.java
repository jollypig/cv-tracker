package com.example.cv.cv;

import com.example.cv.person.Person;
import com.example.cv.person.PersonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Transactional
public class CvDuplicationService {

    private final CvRepository cvRepository;
    private final PersonRepository personRepository;
    private final CvContentService contentService;
    private final CvVersionRepository versionRepository;
    private final CvVersionService versionService;
    private final CvVersionSnapshotSerializer snapshotSerializer;

    public CvDuplicationService(CvRepository cvRepository, PersonRepository personRepository,
            CvContentService contentService,
            CvVersionRepository versionRepository, CvVersionService versionService,
            CvVersionSnapshotSerializer snapshotSerializer) {
        this.cvRepository = cvRepository;
        this.personRepository = personRepository;
        this.contentService = contentService;
        this.versionRepository = versionRepository;
        this.versionService = versionService;
        this.snapshotSerializer = snapshotSerializer;
    }

    public CvResponse duplicate(UUID cvId, Integer sourceVersionNumber, String requestedName,
            UUID targetPersonId, UUID ownerId) {
        Cv source = cvRepository.findById(cvId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));
        Person targetPerson = targetPersonId == null ? source.getPerson()
                : personRepository.findByIdAndOwner_Id(targetPersonId, ownerId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));
        CvVersion parentVersion;
        CvVersionSnapshot sourceSnapshot;
        if (sourceVersionNumber == null) {
            parentVersion = null;
            sourceSnapshot = null;
        } else {
            parentVersion = versionRepository.findByCv_IdAndVersionNumber(cvId, sourceVersionNumber)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV version not found"));
            sourceSnapshot = snapshotSerializer.deserialize(parentVersion.getSnapshot());
        }
        CvContent sourceContent = sourceSnapshot == null ? contentService.get(cvId) : sourceSnapshot.content();
        String sourceName = sourceSnapshot == null ? source.getName() : sourceSnapshot.name();
        String sourceDescription = sourceSnapshot == null ? source.getDescription() : sourceSnapshot.description();
        String sourceLanguage = sourceSnapshot == null ? source.getLanguage() : sourceSnapshot.language();
        UUID sourceTemplateId = sourceSnapshot == null ? source.getTemplateId() : sourceSnapshot.templateId();
        String name = requestedName == null || requestedName.isBlank()
                ? sourceName + " (Copy)"
                : requestedName.trim();

        Cv copy = new Cv(targetPerson, name, sourceLanguage, CvStatus.DRAFT);
        copy.setDescription(sourceDescription);
        copy.setTemplateId(sourceTemplateId);
        copy = cvRepository.save(copy);
        contentService.replace(copy.getId(), sourceContent);
        CvVersionResponse initialVersion = versionService.create(copy.getId(),
                "Copied from " + sourceName, parentVersion == null ? null : parentVersion.getId());

        return new CvResponse(copy.getId(), targetPerson.getId(),
            targetPerson.getFirstName() + " " + targetPerson.getLastName(),
                copy.getName(), copy.getDescription(), copy.getLanguage(), copy.getStatus(), copy.getTemplateId(),
                initialVersion.id(), copy.getCreatedAt(), copy.getUpdatedAt());
    }
}