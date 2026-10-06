package com.example.cv.person;

import com.example.cv.auth.AuthenticatedUser;
import com.example.cv.storage.FileStorage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class PersonService {

    private final PersonRepository personRepository;
    private final FileStorage storage;

    public PersonService(PersonRepository personRepository, FileStorage storage) {
        this.personRepository = personRepository;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<PersonResponse> findAll() {
        return personRepository.findAllByOrderByLastNameAscFirstNameAscIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PersonResponse> findAll(UUID ownerId) {
        return personRepository.findAllByOwner_IdOrderByLastNameAscFirstNameAscIdAsc(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonResponse findById(UUID id) {
        return toResponse(getPerson(id));
    }

    @Transactional(readOnly = true)
    public PersonResponse findById(UUID id, UUID ownerId) {
        return toResponse(getOwnedPerson(id, ownerId));
    }

    public PersonResponse create(PersonRequest request) {
        Person person = new Person(request.firstName().trim(), request.lastName().trim());
        apply(person, request);
        return toResponse(personRepository.save(person));
    }

    public PersonResponse create(PersonRequest request, AuthenticatedUser owner) {
        Person person = new Person(request.firstName().trim(), request.lastName().trim());
        person.setOwner(owner);
        apply(person, request);
        return toResponse(personRepository.save(person));
    }

    public PersonResponse update(UUID id, PersonRequest request) {
        Person person = getPerson(id);
        person.setFirstName(request.firstName().trim());
        person.setLastName(request.lastName().trim());
        apply(person, request);
        return toResponse(personRepository.save(person));
    }

    public PersonResponse update(UUID id, PersonRequest request, UUID ownerId) {
        Person person = getOwnedPerson(id, ownerId);
        person.setFirstName(request.firstName().trim());
        person.setLastName(request.lastName().trim());
        apply(person, request);
        return toResponse(personRepository.save(person));
    }

    public void delete(UUID id) {
        Person person = getPerson(id);
        personRepository.delete(person);
    }

    public void delete(UUID id, UUID ownerId) {
        personRepository.delete(getOwnedPerson(id, ownerId));
    }

    public PersonResponse uploadPhoto(UUID id, MultipartFile file, UUID ownerId) {
        Person person = getOwnedPerson(id, ownerId);
        if (file == null || file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo must be between 1 byte and 5 MB");
        }

        String contentType = file.getContentType();
        String extension = "image/png".equalsIgnoreCase(contentType) ? "png"
                : "image/jpeg".equalsIgnoreCase(contentType) ? "jpg" : null;
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo must be a JPEG or PNG image");
        }

        String storageKey = "persons/" + id + "/" + UUID.randomUUID() + "." + extension;
        try (var input = file.getInputStream()) {
            storage.upload(storageKey, input, contentType.toLowerCase(Locale.ROOT));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read uploaded photo", exception);
        }

        person.setPhotoStorageKey(storageKey);
        Person savedPerson = personRepository.save(person);
        return toResponse(savedPerson);
    }

    @Transactional(readOnly = true)
    public PersonPhoto getPhoto(UUID id, UUID ownerId) {
        Person person = getOwnedPerson(id, ownerId);
        String storageKey = person.getPhotoStorageKey();
        if (storageKey == null || !isPhotoKeyForPerson(storageKey, id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Person photo not found");
        }
        try (var content = storage.download(storageKey)) {
            String contentType = storageKey.endsWith(".png") ? "image/png" : "image/jpeg";
            return new PersonPhoto(content.readAllBytes(), contentType);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read stored photo", exception);
        }
    }

    private Person getPerson(UUID id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));
    }

    private Person getOwnedPerson(UUID id, UUID ownerId) {
        return personRepository.findByIdAndOwner_Id(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));
    }

    private void apply(Person person, PersonRequest request) {
        person.setDateOfBirth(request.dateOfBirth());
        person.setPosition(blankToNull(request.position()));
        person.setGender(blankToNull(request.gender()));
        person.setMaritalStatus(blankToNull(request.maritalStatus()));
        person.setMilitaryStatus(blankToNull(request.militaryStatus()));
        person.setLocation(blankToNull(request.location()));
        List<PersonContact> contacts = request.contacts() == null ? List.of() : request.contacts().stream()
                .map(contact -> new PersonContact(
                        contact.type().name(), contact.value().trim(), contact.primary(), contact.sortOrder()))
                .toList();
        person.setContacts(contacts);
    }

    private PersonResponse toResponse(Person person) {
        List<PersonContactResponse> contacts = person.getContacts().stream()
                .sorted(Comparator.comparingInt(PersonContact::getSortOrder))
                .map(contact -> new PersonContactResponse(
                        contact.getId(), ContactType.valueOf(contact.getType()), contact.getValue(),
                        contact.isPrimary(), contact.getSortOrder()))
                .toList();
        return new PersonResponse(person.getId(), person.getFirstName(), person.getLastName(),
            person.getDateOfBirth(), person.getPosition(), person.getGender(),
            person.getMaritalStatus(), person.getMilitaryStatus(), person.getLocation(),
            person.getPhotoStorageKey(), contacts,
                person.getCreatedAt(), person.getUpdatedAt());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isPhotoKeyForPerson(String storageKey, UUID personId) {
        return storageKey.matches("persons/" + personId + "/[0-9a-fA-F-]{36}\\.(jpg|png)");
    }
}