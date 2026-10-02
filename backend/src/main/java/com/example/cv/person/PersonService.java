package com.example.cv.person;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PersonService {

    private final PersonRepository personRepository;

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @Transactional(readOnly = true)
    public List<PersonResponse> findAll() {
        return personRepository.findAll(Sort.by("lastName", "firstName", "id")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonResponse findById(UUID id) {
        return toResponse(getPerson(id));
    }

    public PersonResponse create(PersonRequest request) {
        Person person = new Person(request.firstName().trim(), request.lastName().trim());
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

    public void delete(UUID id) {
        Person person = getPerson(id);
        personRepository.delete(person);
    }

    private Person getPerson(UUID id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));
    }

    private void apply(Person person, PersonRequest request) {
        person.setDateOfBirth(request.dateOfBirth());
        person.setPosition(blankToNull(request.position()));
        person.setGender(blankToNull(request.gender()));
        person.setMaritalStatus(blankToNull(request.maritalStatus()));
        person.setMilitaryStatus(blankToNull(request.militaryStatus()));
        person.setLocation(blankToNull(request.location()));
        person.setPhotoStorageKey(blankToNull(request.photoStorageKey()));
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
}