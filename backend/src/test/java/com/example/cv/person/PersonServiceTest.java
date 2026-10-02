package com.example.cv.person;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock
    private PersonRepository personRepository;

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
}