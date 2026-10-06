package com.example.cv.person;

import com.example.cv.auth.AuthenticatedUser;
import com.example.cv.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private FileStorage storage;

    @InjectMocks
    private PersonService personService;

    @Test
    void createsPersonWithContacts() {
        PersonRequest request = new PersonRequest(" Ada ", " Lovelace ", LocalDate.of(1815, 12, 10),
            "Analyst", "Female", "Single", "Completed", "London", null,
                List.of(new PersonContactRequest(ContactType.EMAIL, "ada@example.com", true, 0)));
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PersonResponse response = personService.create(request);

        ArgumentCaptor<Person> captor = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(captor.capture());
        assertEquals("Ada", captor.getValue().getFirstName());
        assertEquals("Lovelace", captor.getValue().getLastName());
        assertEquals("Analyst", response.position());
        assertEquals("Female", response.gender());
        assertEquals("Single", response.maritalStatus());
        assertEquals("Completed", response.militaryStatus());
        assertEquals("London", response.location());
        assertEquals("EMAIL", captor.getValue().getContacts().get(0).getType());
        assertEquals("ada@example.com", response.contacts().get(0).value());
    }

    @Test
    void assignsTheAuthenticatedOwnerWhenCreatingPerson() {
        AuthenticatedUser owner = org.mockito.Mockito.mock(AuthenticatedUser.class);
        PersonRequest request = new PersonRequest("Ada", "Lovelace", null,
                null, null, null, null, null, null, List.of());
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        personService.create(request, owner);

        ArgumentCaptor<Person> captor = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(captor.capture());
        assertSame(owner, captor.getValue().getOwner());
    }

    @Test
    void listsOnlyPeopleOwnedByTheRequestedAccount() {
        UUID ownerId = UUID.randomUUID();
        when(personRepository.findAllByOwner_IdOrderByLastNameAscFirstNameAscIdAsc(ownerId))
                .thenReturn(List.of(new Person("Ada", "Lovelace")));

        assertEquals(1, personService.findAll(ownerId).size());
        verify(personRepository).findAllByOwner_IdOrderByLastNameAscFirstNameAscIdAsc(ownerId);
    }

    @Test
    void updateReplacesThePersonContactList() {
        UUID id = UUID.randomUUID();
        Person person = new Person("Ada", "Lovelace");
        when(personRepository.findById(id)).thenReturn(Optional.of(person));
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PersonRequest request = new PersonRequest("Augusta", "King", null, null,
            null, null, null, null, null, List.of());

        PersonResponse response = personService.update(id, request);

        assertEquals("Augusta", response.firstName());
        assertEquals(List.of(), response.contacts());
    }

    @Test
    void missingPersonReturnsNotFound() {
        UUID id = UUID.randomUUID();
        when(personRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> personService.findById(id));
    }

    @Test
    void uploadsPhotoAndPersistsGeneratedStorageKey() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Person person = new Person("Ada", "Lovelace");
        ReflectionTestUtils.setField(person, "id", id);
        when(personRepository.findByIdAndOwner_Id(id, ownerId)).thenReturn(Optional.of(person));
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile image = new MockMultipartFile("file", "profile.png", "image/png", new byte[]{1, 2, 3});

        PersonResponse response = personService.uploadPhoto(id, image, ownerId);

        assertTrue(response.photoStorageKey().endsWith(".png"));
        assertEquals(response.photoStorageKey(), person.getPhotoStorageKey());
        verify(storage).upload(eq(response.photoStorageKey()), any(java.io.InputStream.class), eq("image/png"));
    }
}