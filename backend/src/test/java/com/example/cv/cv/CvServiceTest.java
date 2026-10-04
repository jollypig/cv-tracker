package com.example.cv.cv;

import com.example.cv.person.Person;
import com.example.cv.person.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CvServiceTest {

    @Mock
    private CvRepository cvRepository;

    @Mock
    private PersonRepository personRepository;

    @InjectMocks
    private CvService cvService;

    @Test
    void createsCvForPersonAndNormalizesOptionalDescription() {
        UUID personId = UUID.randomUUID();
        Person person = new Person("Ada", "Lovelace");
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(cvRepository.save(any(Cv.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CvResponse response = cvService.create(personId,
            new CvRequest(" Backend ", "  ", " en ", CvStatus.DRAFT, List.of(" backend ", "platform")));

        ArgumentCaptor<Cv> captor = ArgumentCaptor.forClass(Cv.class);
        verify(cvRepository).save(captor.capture());
        assertEquals(person, captor.getValue().getPerson());
        assertEquals("Backend", response.name());
        assertEquals("Ada Lovelace", response.personName());
        assertEquals("en", response.language());
        assertEquals(null, response.description());
        assertEquals(CvStatus.DRAFT, response.status());
        assertEquals(List.of("backend", "platform"), response.tags());
        assertEquals(List.of("backend", "platform"), captor.getValue().getTags().stream().sorted().toList());
    }

    @Test
    void listsCvsForPerson() {
        UUID personId = UUID.randomUUID();
        when(personRepository.findById(personId)).thenReturn(Optional.of(new Person("Ada", "Lovelace")));
        when(cvRepository.findAllByPersonIdOrderByUpdatedAtDesc(personId)).thenReturn(List.of());

        assertEquals(List.of(), cvService.findAll(personId));
        verify(cvRepository).findAllByPersonIdOrderByUpdatedAtDesc(personId);
    }

    @Test
    void listsOnlyCvsOwnedByTheAuthenticatedAccount() {
        UUID ownerId = UUID.randomUUID();
        when(cvRepository.findAllByPerson_Owner_IdOrderByUpdatedAtDesc(ownerId)).thenReturn(List.of());

        assertEquals(List.of(), cvService.findAll(null, ownerId));
        verify(cvRepository).findAllByPerson_Owner_IdOrderByUpdatedAtDesc(ownerId);
    }

    @Test
    void missingPersonReturnsNotFoundWhenCreatingCv() {
        UUID personId = UUID.randomUUID();
        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> cvService.create(personId,
            new CvRequest("Backend", null, "en", CvStatus.DRAFT, null)));
    }
}