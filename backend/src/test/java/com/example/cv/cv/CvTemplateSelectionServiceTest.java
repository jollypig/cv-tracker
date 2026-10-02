package com.example.cv.cv;

import com.example.cv.person.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CvTemplateSelectionServiceTest {

    @Mock
    private CvRepository cvRepository;

    @Mock
    private CvTemplateRepository templateRepository;

    @InjectMocks
    private CvTemplateSelectionService selectionService;

    @Test
    void selectTemplateStoresAndReturnsTheSelectedTemplate() {
        UUID cvId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        Cv cv = mock(Cv.class);
        CvTemplate template = mock(CvTemplate.class);
        Person person = mock(Person.class);
        when(cvRepository.findById(cvId)).thenReturn(Optional.of(cv));
        when(templateRepository.findByIdAndActiveTrue(templateId)).thenReturn(Optional.of(template));
        when(template.getId()).thenReturn(templateId);
        when(cvRepository.save(cv)).thenReturn(cv);
        when(cv.getPerson()).thenReturn(person);
        when(cv.getTemplateId()).thenReturn(templateId);

        CvResponse response = selectionService.selectTemplate(cvId, templateId);

        assertEquals(templateId, response.templateId());
        verify(cv).setTemplateId(templateId);
        verify(cvRepository).save(cv);
    }

    @Test
    void selectTemplateReturnsNotFoundForInactiveOrMissingTemplate() {
        UUID cvId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        when(cvRepository.findById(cvId)).thenReturn(Optional.of(mock(Cv.class)));
        when(templateRepository.findByIdAndActiveTrue(templateId)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> selectionService.selectTemplate(cvId, templateId));

        assertEquals(404, error.getStatusCode().value());
    }
}